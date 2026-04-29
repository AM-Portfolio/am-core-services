package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.ActivityFilter;
import com.am.analysis.dto.ActivityItem;
import com.am.analysis.dto.ActivityType;
import com.am.analysis.dto.DashboardSummary;
import com.am.analysis.dto.RecentActivityResponse;
import com.am.analysis.service.aggregator.AnalysisAggregator;
import com.am.domain.trade.PortfolioOverview;
import com.am.kafka.config.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardAnalysisService {

    private final AnalysisAggregator aggregator;
    private final AnalysisRepository analysisRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DashboardSummary getSummary(String userId) {
        log.info("[DashboardAnalysisService] Fetching summary for userId: {}", userId);
        return aggregator.getOverallSummary(userId);
    }

    public List<PortfolioOverview> getPortfolioOverviews(String userId) {
        log.info("[DashboardAnalysisService] Fetching portfolio overviews for userId: {}", userId);
        return aggregator.getPortfolioOverviews(userId);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Recent Activity — Real Implementation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Backward-compatible overload: returns the first 20 items with default filter.
     * Used by existing callers that haven't migrated to the paginated API yet.
     */
    public List<ActivityItem> getRecentActivity(String userId) {
        return getRecentActivity(userId, ActivityFilter.builder().build()).getItems();
    }

    /**
     * Primary method: returns a paginated, filtered, sorted feed of portfolio holdings.
     *
     * Data source: AnalysisRepository → AnalysisHolding (live portfolio positions).
     * Each holding becomes an ActivityItem with:
     *   - symbol, companyName, exchange, sector
     *   - avgBuyingPrice, currentPrice, quantity, investmentValue, currentValue
     *   - profitLoss, profitLossPercent, dayChange, dayChangePercent
     *   - status: WIN / LOSS / NEUTRAL
     *
     * Filtering: by type, status (WIN/LOSS), sector, portfolioName
     * Sorting:   TIMESTAMP | PROFIT_LOSS | PROFIT_LOSS_ASC | DAY_CHANGE | CURRENT_VALUE
     * Pagination: page + size
     */
    public RecentActivityResponse getRecentActivity(String userId, ActivityFilter filter) {
        log.info("[DashboardAnalysisService] Processing recent activity request for userId: {} with filter: {}", userId, filter);

        // 1. Load all analysis entities for this user (PORTFOLIO type = live holdings)
        List<AnalysisEntity> entities = analysisRepository.findByOwnerIdAndType(userId, AnalysisEntityType.PORTFOLIO);
        log.debug("[DashboardAnalysisService] Found {} analysis entities for userId: {}", entities.size(), userId);

        // 2. Flatten all holdings across all portfolios → ActivityItems
        List<ActivityItem> allItems = new ArrayList<>();
        for (AnalysisEntity entity : entities) {
            // portfolioId = the canonical ID of the portfolio (sourceId)
            String portfolioId   = entity.getSourceId();
            String portfolioName = entity.getSourceId(); // enrichment possible if name stored
            LocalDateTime lastUpdated = entity.getLastUpdated();

            if (entity.getHoldings() == null) continue;

            for (AnalysisHolding holding : entity.getHoldings()) {
                ActivityItem item = mapHoldingToActivity(holding, portfolioId, portfolioName, lastUpdated);
                if (item != null) allItems.add(item);
            }
        }

        // 3. Apply filters
        List<ActivityItem> filtered = applyFilters(allItems, filter);
        log.debug("[DashboardAnalysisService] Total holdings: {}, After filtering: {}", allItems.size(), filtered.size());

        // 4. Compute summary counters (on unfiltered by status so counts are always full)
        int totalWin     = (int) allItems.stream().filter(i -> "WIN".equals(i.getStatus())).count();
        int totalLoss    = (int) allItems.stream().filter(i -> "LOSS".equals(i.getStatus())).count();
        int totalNeutral = (int) allItems.stream().filter(i -> "NEUTRAL".equals(i.getStatus())).count();

        // 5. Sort
        List<ActivityItem> sorted = applySorting(filtered, filter.getSortBy());

        // 6. Paginate
        int totalItems = sorted.size();
        int size = Math.min(filter.getSize(), 100); // cap at 100
        int page = filter.getPage();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalItems / size) : 1;

        int fromIndex = Math.min(page * size, totalItems);
        int toIndex   = Math.min(fromIndex + size, totalItems);
        List<ActivityItem> pageItems = sorted.subList(fromIndex, toIndex);

        return RecentActivityResponse.builder()
                .items(pageItems)
                .page(page)
                .size(size)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .totalWinning(totalWin)
                .totalLosing(totalLoss)
                .totalNeutral(totalNeutral)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mapping
    // ─────────────────────────────────────────────────────────────────────

    private ActivityItem mapHoldingToActivity(AnalysisHolding holding, String portfolioId, String portfolioName, LocalDateTime lastUpdated) {
        if (holding.getIdentity() == null) return null;

        String symbol      = holding.getIdentity().getSymbol();
        String companyName = StringUtils.hasText(holding.getIdentity().getCompanyName())
                ? holding.getIdentity().getCompanyName()
                : holding.getIdentity().getName();
        String exchange = holding.getIdentity().getExchange();
        String sector   = holding.getClassification() != null ? holding.getClassification().getSector() : null;

        InvestmentStats inv = holding.getInvestment();
        MarketStats     mkt = holding.getMarket();

        Double avgBuyingPrice  = inv != null ? inv.getAveragePrice()         : null;
        Double quantity        = inv != null ? inv.getQuantity()              : null;
        Double investmentValue = inv != null ? inv.getInvestmentValue()       : null;
        Double currentValue    = inv != null ? inv.getCurrentValue()          : null;
        Double profitLoss      = inv != null ? inv.getProfitLoss()            : null;
        Double profitLossPct   = inv != null ? inv.getProfitLossPercentage()  : null;
        Double currentPrice    = mkt != null ? mkt.getCurrentPrice()          : null;
        Double dayChange       = mkt != null ? mkt.getDayChange()             : null;
        Double dayChangePct    = mkt != null ? mkt.getDayChangePercentage()   : null;

        String status = ActivityItem.resolveStatus(profitLoss);

        // Human-readable title
        String title = symbol != null ? symbol : "Unknown";
        if (companyName != null) title = companyName;

        String description = buildDescription(quantity, avgBuyingPrice, profitLossPct);

        return ActivityItem.builder()
                .id(UUID.randomUUID().toString())
                .type(ActivityType.HOLDING)
                .portfolioId(portfolioId)
                .portfolioName(portfolioName)
                .symbol(symbol)
                .companyName(companyName)
                .exchange(exchange)
                .sector(sector)
                .quantity(quantity)
                .avgBuyingPrice(avgBuyingPrice)
                .currentPrice(currentPrice)
                .investmentValue(investmentValue)
                .currentValue(currentValue)
                .profitLoss(profitLoss)
                .profitLossPercent(profitLossPct)
                .dayChange(dayChange)
                .dayChangePercent(dayChangePct)
                .status(status)
                .title(title)
                .description(description)
                .timestamp(lastUpdated != null ? lastUpdated : LocalDateTime.now())
                .build();
    }

    private String buildDescription(Double quantity, Double avgPrice, Double profitLossPct) {
        StringBuilder sb = new StringBuilder();
        if (quantity != null)  sb.append(String.format("%.2f units", quantity));
        if (avgPrice != null)  sb.append(String.format(" @ ₹%.2f avg", avgPrice));
        if (profitLossPct != null) {
            sb.append(String.format(" • %s%.2f%%", profitLossPct >= 0 ? "+" : "", profitLossPct));
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Filter
    // ─────────────────────────────────────────────────────────────────────

    private List<ActivityItem> applyFilters(List<ActivityItem> items, ActivityFilter filter) {
        return items.stream()
                .filter(i -> !StringUtils.hasText(filter.getType())          || ActivityType.valueOf(filter.getType()) == i.getType())
                .filter(i -> !StringUtils.hasText(filter.getStatus())        || filter.getStatus().equalsIgnoreCase(i.getStatus()))
                .filter(i -> !StringUtils.hasText(filter.getSector())        || filter.getSector().equalsIgnoreCase(i.getSector()))
                .filter(i -> !StringUtils.hasText(filter.getPortfolioName()) || (i.getPortfolioName() != null && i.getPortfolioName().contains(filter.getPortfolioName())))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sort
    // ─────────────────────────────────────────────────────────────────────

    private List<ActivityItem> applySorting(List<ActivityItem> items, String sortBy) {
        Comparator<ActivityItem> comparator = switch (sortBy == null ? "TIMESTAMP" : sortBy.toUpperCase()) {
            case "PROFIT_LOSS"      -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getProfitLoss() != null ? i.getProfitLoss() : 0.0).reversed();
            case "PROFIT_LOSS_ASC"  -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getProfitLoss() != null ? i.getProfitLoss() : 0.0);
            case "DAY_CHANGE"       -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getDayChange() != null ? i.getDayChange() : 0.0).reversed();
            case "CURRENT_VALUE"    -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getCurrentValue() != null ? i.getCurrentValue() : 0.0).reversed();
            default /* TIMESTAMP */ -> Comparator.comparing(ActivityItem::getTimestamp,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
        return items.stream().sorted(comparator).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Dashboard Update Publisher
    // ─────────────────────────────────────────────────────────────────────

    public void publishDashboardUpdate(String userId) {
        try {
            log.info("[DashboardAnalysisService] Preparing dashboard update event for userId: {}", userId);
            DashboardSummary summary = getSummary(userId);
            DashboardUpdateEvent event = new DashboardUpdateEvent(userId, summary);
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.DASHBOARD_UPDATE, payload);
            log.info("[DashboardAnalysisService] Successfully published dashboard update for user: {} to topic: {}", userId, KafkaTopics.DASHBOARD_UPDATE);
        } catch (Exception e) {
            log.error("[DashboardAnalysisService] Failed to publish dashboard update for user: {}", userId, e);
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DashboardUpdateEvent {
        private String userId;
        private DashboardSummary summary;
    }
}
