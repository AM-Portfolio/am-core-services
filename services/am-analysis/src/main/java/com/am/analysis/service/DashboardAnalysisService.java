package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.DashboardWidgetType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.BootstrapTrigger;
import com.am.analysis.service.load.EntityLoadResult;
import com.am.analysis.dto.ActivityFilter;
import com.am.analysis.dto.ActivityItem;
import com.am.analysis.dto.ActivityType;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.DashboardSummary;
import com.am.analysis.dto.RecentActivityResponse;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.aggregator.AnalysisAggregator;
import com.am.analysis.metrics.AnalysisBusinessMetrics;
import com.am.analysis.service.impl.AllocationAnalysisService;
import com.am.analysis.service.impl.PerformanceAnalysisService;
import com.am.analysis.service.impl.TopMoversAnalysisService;
import com.am.domain.trade.PortfolioOverview;
import com.am.kafka.config.KafkaTopics;
import com.am.kafka.config.Timeframe;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
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
    private final AnalysisEntityLoadService entityLoadService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;
    private final DashboardSnapshotService snapshotService;
    private final AllocationAnalysisService allocationService;
    private final TopMoversAnalysisService topMoversService;
    private final PerformanceAnalysisService performanceService;
    private final GlobalPortfolioResolver globalPortfolioResolver;
    private final AnalysisBusinessMetrics businessMetrics;

    public DashboardSummary getSummary(String userId) {
        return snapshotService.load(userId, DashboardWidgetType.SUMMARY, DashboardSummary.class)
                .orElseGet(() -> {
                    log.info("[Summary] Snapshot miss for user {}, computing live", userId);
                    DashboardSummary summary = aggregator.getOverallSummary(userId);
                    if (summary != null) {
                        snapshotService.persist(userId, DashboardWidgetType.SUMMARY, summary);
                    }
                    return summary;
                });
    }

    public List<PortfolioOverview> getPortfolioOverviews(String userId) {
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
        boolean isDefaultFilter = filter.getPage() == 0 && filter.getSize() == 20 &&
                !StringUtils.hasText(filter.getType()) && !StringUtils.hasText(filter.getStatus()) &&
                !StringUtils.hasText(filter.getSector()) && !StringUtils.hasText(filter.getPortfolioName());

        if (isDefaultFilter) {
            return snapshotService.load(userId, DashboardWidgetType.ACTIVITY, RecentActivityResponse.class)
                    .orElseGet(() -> {
                        log.info("[Activity] Snapshot miss for user {}, computing live", userId);
                        RecentActivityResponse response = getRecentActivityUncached(userId, filter);
                        if (response != null) {
                            snapshotService.persist(userId, DashboardWidgetType.ACTIVITY, response);
                        }
                        return response;
                    });
        }
        return getRecentActivityUncached(userId, filter);
    }

    private RecentActivityResponse getRecentActivityUncached(String userId, ActivityFilter filter) {
        return getRecentActivityUncached(userId, filter, Map.of());
    }

    private RecentActivityResponse getRecentActivityUncached(String userId, ActivityFilter filter,
                                                               Map<String, LivePriceTick> liveTicks) {
        log.info("[DashboardAnalysisService] Processing recent activity request for userId: {} with filter: {}", userId, filter);

        // 1. Load all analysis entities for this user (PORTFOLIO type = live holdings)
        EntityLoadResult loadResult = entityLoadService.loadPortfoliosForUser(userId, BootstrapTrigger.DASHBOARD);
        List<AnalysisEntity> entities = loadResult.entities();
        if (liveTicks != null && !liveTicks.isEmpty()) {
            LivePriceOverlayHelper.applyAll(entities, liveTicks);
        }
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

        String symbol = holding.getIdentity().getSymbol();
        if (!StringUtils.hasText(symbol)) {
            symbol = StringUtils.hasText(holding.getIdentity().getIsin())
                    ? holding.getIdentity().getIsin()
                    : holding.getIdentity().getName();
        }

        String companyName = StringUtils.hasText(holding.getIdentity().getCompanyName())
                ? holding.getIdentity().getCompanyName()
                : holding.getIdentity().getName();
        String exchange = holding.getIdentity().getExchange();
        String sector   = holding.getClassification() != null ? holding.getClassification().getSector() : null;

        InvestmentStats inv = holding.getInvestment();
        MarketStats     mkt = holding.getMarket();

        Double quantity        = inv != null ? inv.getQuantity()              : null;
        Double investmentValue = inv != null ? inv.getInvestmentValue()       : null;
        Double avgBuyingPrice  = inv != null ? inv.getAveragePrice()         : null;

        if ((avgBuyingPrice == null || avgBuyingPrice == 0.0) 
                && investmentValue != null && quantity != null && quantity > 0) {
            avgBuyingPrice = investmentValue / quantity;
        }

        Double currentValue    = inv != null ? inv.getCurrentValue()          : null;
        Double profitLoss      = inv != null ? inv.getProfitLoss()            : null;
        Double profitLossPct   = inv != null ? inv.getProfitLossPercentage()  : null;
        Double currentPrice    = mkt != null ? mkt.getCurrentPrice()          : null;
        Double dayChange       = mkt != null ? mkt.getDayChange()             : null;
        Double dayChangePct    = mkt != null ? mkt.getDayChangePercentage()   : null;

        if (currentPrice != null && currentPrice > 0 && avgBuyingPrice != null && avgBuyingPrice > 0) {
            double qty = (quantity != null && quantity > 0) ? quantity : 1.0;
            currentValue = currentPrice * qty;
            investmentValue = avgBuyingPrice * qty;
            profitLoss = currentValue - investmentValue;
            profitLossPct = ((currentPrice - avgBuyingPrice) / avgBuyingPrice) * 100.0;
        }

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
            case "PROFIT_LOSS_PERCENT" -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getProfitLossPercent() != null ? i.getProfitLossPercent() : 0.0).reversed();
            case "DAY_CHANGE"       -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getDayChange() != null ? i.getDayChange() : 0.0).reversed();
            case "DAY_CHANGE_PERCENT" -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getDayChangePercent() != null ? i.getDayChangePercent() : 0.0).reversed();
            case "CURRENT_VALUE"    -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getCurrentValue() != null ? i.getCurrentValue() : 0.0).reversed();
            case "SYMBOL"           -> Comparator.comparing(
                    (ActivityItem i) -> i.getSymbol() != null ? i.getSymbol() : "",
                    String.CASE_INSENSITIVE_ORDER);
            case "QUANTITY"         -> Comparator.comparingDouble((ActivityItem i) ->
                    i.getQuantity() != null ? i.getQuantity() : 0.0).reversed();
            default /* TIMESTAMP */ -> Comparator.comparing(ActivityItem::getTimestamp,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
        return items.stream().sorted(comparator).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Dashboard Update Publisher
    // ─────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────
    // Dashboard Update Publisher
    // ─────────────────────────────────────────────────────────────────────

    public void publishDashboardUpdate(String userId) {
        publishDashboardSummary(userId);
        publishDashboardActivity(userId);
        publishDashboardAllocation(userId);
        publishDashboardMovers(userId);
    }

    /** Immediate push of all 5 dashboard widgets on subscribe (no debounce). */
    public void publishDashboardSubscribeAll(String userId) {
        publishDashboardUpdate(userId);
        publishDashboardHistory(userId, Timeframe.ONE_DAY);
    }

    public void publishDashboardHistory(String userId, Timeframe window) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.dashboard_history",
                "userId", userId, "window", window.getCode(), "topic", KafkaTopics.DASHBOARD_HISTORY_UPDATE)) {
            try {
                PerformanceResponse history = performanceService.getPerformance(
                        globalPortfolioResolver.globalSourceId(),
                        AnalysisEntityType.PORTFOLIO,
                        window.getCode(),
                        userId);
                if (history != null) {
                    snapshotService.persist(userId, DashboardWidgetType.HISTORY, history);

                    WidgetUpdateEvent event = new WidgetUpdateEvent(userId, history);
                    String payload = objectMapper.writeValueAsString(event);
                    kafkaTemplate.send(KafkaTopics.DASHBOARD_HISTORY_UPDATE, userId, payload);

                    flowLogger.complete(span, "payload_bytes", payload.length());
                    businessMetrics.dashboardWidgetPublish("history", "success");
                } else {
                    flowLogger.fail(span, null, "reason", "null_history");
                    businessMetrics.dashboardWidgetPublish("history", "failure");
                }
            } catch (Exception e) {
                flowLogger.fail(span, e);
                businessMetrics.dashboardWidgetPublish("history", "failure");
            }
        }
    }

    public void publishDashboardSummary(String userId) {
        publishDashboardSummary(userId, Map.of());
    }

    public void publishDashboardSummary(String userId, Map<String, LivePriceTick> liveTicks) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.dashboard_summary",
                "userId", userId, "topic", KafkaTopics.DASHBOARD_SUMMARY_UPDATE)) {
            try {
                DashboardSummary summary = aggregator.getOverallSummary(userId, liveTicks);
                if (summary != null) {
                    snapshotService.persist(userId, DashboardWidgetType.SUMMARY, summary);
                    
                    WidgetUpdateEvent event = new WidgetUpdateEvent(userId, summary);
                    String payload = objectMapper.writeValueAsString(event);
                    kafkaTemplate.send(KafkaTopics.DASHBOARD_SUMMARY_UPDATE, userId, payload);
                    
                    flowLogger.complete(span, "payload_bytes", payload.length());
                    businessMetrics.dashboardWidgetPublish("summary", "success");
                } else {
                    flowLogger.fail(span, null, "reason", "null_summary");
                    businessMetrics.dashboardWidgetPublish("summary", "failure");
                }
            } catch (Exception e) {
                flowLogger.fail(span, e);
                businessMetrics.dashboardWidgetPublish("summary", "failure");
            }
        }
    }

    public void publishDashboardActivity(String userId) {
        publishDashboardActivity(userId, Map.of());
    }

    public void publishDashboardActivity(String userId, Map<String, LivePriceTick> liveTicks) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.dashboard_activity",
                "userId", userId, "topic", KafkaTopics.DASHBOARD_ACTIVITY_UPDATE)) {
            try {
                RecentActivityResponse activity = getRecentActivityUncached(userId,
                        ActivityFilter.builder().size(10).sortBy("TIMESTAMP").build(), liveTicks);
                if (activity != null) {
                    snapshotService.persist(userId, DashboardWidgetType.ACTIVITY, activity);
                    
                    WidgetUpdateEvent event = new WidgetUpdateEvent(userId, activity);
                    String payload = objectMapper.writeValueAsString(event);
                    kafkaTemplate.send(KafkaTopics.DASHBOARD_ACTIVITY_UPDATE, userId, payload);
                    
                    flowLogger.complete(span, "payload_bytes", payload.length());
                    businessMetrics.dashboardWidgetPublish("activity", "success");
                } else {
                    flowLogger.fail(span, null, "reason", "null_activity");
                    businessMetrics.dashboardWidgetPublish("activity", "failure");
                }
            } catch (Exception e) {
                flowLogger.fail(span, e);
                businessMetrics.dashboardWidgetPublish("activity", "failure");
            }
        }
    }

    public void publishDashboardAllocation(String userId) {
        publishDashboardAllocation(userId, Map.of());
    }

    public void publishDashboardAllocation(String userId, Map<String, LivePriceTick> liveTicks) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.dashboard_allocation",
                "userId", userId, "topic", KafkaTopics.DASHBOARD_ALLOCATION_UPDATE)) {
            try {
                AllocationResponse allocation = allocationService.getAllocation("ALL", AnalysisEntityType.PORTFOLIO, userId, AnalysisGroupBy.SECTOR, liveTicks);
                if (allocation != null) {
                    snapshotService.persist(userId, DashboardWidgetType.ALLOCATION, allocation);
                    
                    WidgetUpdateEvent event = new WidgetUpdateEvent(userId, allocation);
                    String payload = objectMapper.writeValueAsString(event);
                    kafkaTemplate.send(KafkaTopics.DASHBOARD_ALLOCATION_UPDATE, userId, payload);
                    
                    flowLogger.complete(span, "payload_bytes", payload.length());
                    businessMetrics.dashboardWidgetPublish("allocation", "success");
                } else {
                    flowLogger.fail(span, null, "reason", "null_allocation");
                    businessMetrics.dashboardWidgetPublish("allocation", "failure");
                }
            } catch (Exception e) {
                flowLogger.fail(span, e);
                businessMetrics.dashboardWidgetPublish("allocation", "failure");
            }
        }
    }

    public void publishDashboardMovers(String userId) {
        publishDashboardMovers(userId, Map.of());
    }

    public void publishDashboardMovers(String userId, Map<String, LivePriceTick> liveTicks) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.dashboard_movers",
                "userId", userId, "topic", KafkaTopics.DASHBOARD_MOVERS_UPDATE)) {
            try {
                TopMoversResponse movers = topMoversService.getTopMovers(null, AnalysisEntityType.PORTFOLIO, "1D", userId, AnalysisGroupBy.STOCK, liveTicks);
                if (movers != null) {
                    snapshotService.persist(userId, DashboardWidgetType.MOVERS, movers);
                    
                    WidgetUpdateEvent event = new WidgetUpdateEvent(userId, movers);
                    String payload = objectMapper.writeValueAsString(event);
                    kafkaTemplate.send(KafkaTopics.DASHBOARD_MOVERS_UPDATE, userId, payload);
                    
                    flowLogger.complete(span, "payload_bytes", payload.length());
                    businessMetrics.dashboardWidgetPublish("movers", "success");
                } else {
                    flowLogger.fail(span, null, "reason", "null_movers");
                    businessMetrics.dashboardWidgetPublish("movers", "failure");
                }
            } catch (Exception e) {
                flowLogger.fail(span, e);
                businessMetrics.dashboardWidgetPublish("movers", "failure");
            }
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class WidgetUpdateEvent {
        private String userId;
        private Object data;
    }
}
