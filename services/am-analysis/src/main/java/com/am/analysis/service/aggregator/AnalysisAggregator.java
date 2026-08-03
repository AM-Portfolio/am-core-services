package com.am.analysis.service.aggregator;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.BootstrapTrigger;
import com.am.analysis.service.load.EntityLoadResult;
import com.am.analysis.dto.DashboardSummary;
import com.am.analysis.service.LivePriceOverlayHelper;
import com.am.analysis.service.LivePriceTick;
import com.am.domain.trade.PortfolioOverview;
import com.am.domain.trade.TradePortfolio;
import com.am.observability.flow.FlowLogger;
import com.am.trade.client.service.TradeClientService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates data from am-portfolio (AnalysisRepository) and am-trade
 * (TradeClientService).
 *
 * Key design rules:
 * 1. am-portfolio is the source of truth for live holdings & market values.
 * 2. am-trade fills in trade-specific portfolios NOT already linked via
 * externalPortfolioId.
 * 3. de-duplication: Trade portfolios whose externalPortfolioId matches an
 * am-portfolio ID are SKIPPED.
 * 4. Resilience: CB on each source. isComplete=false when either degrades.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalysisAggregator {

    private final AnalysisEntityLoadService entityLoadService;
    private final TradeClientService tradeClientService;
    private final FlowLogger flowLogger;

    // ─────────────────────────────────────────────────────────────────────
    // Dashboard Summary (header-level aggregate)
    // ─────────────────────────────────────────────────────────────────────

    public DashboardSummary getOverallSummary(String userId) {
        return getOverallSummary(userId, Map.of());
    }

    public DashboardSummary getOverallSummary(String userId, Map<String, LivePriceTick> liveTicks) {
        List<AnalysisEntity> amPortfolios = fetchPortfolioEntities(userId);
        List<TradePortfolio> tradePortfolios = fetchTradePortfolios(userId);

        boolean isComplete = amPortfolios != null && tradePortfolios != null;
        if (amPortfolios == null)
            amPortfolios = Collections.emptyList();
        if (tradePortfolios == null)
            tradePortfolios = Collections.emptyList();

        if (liveTicks != null && !liveTicks.isEmpty()) {
            LivePriceOverlayHelper.applyAll(amPortfolios, liveTicks);
        }

        return buildOverallSummary(amPortfolios, tradePortfolios, isComplete);
    }

    private DashboardSummary buildOverallSummary(List<AnalysisEntity> amPortfolios,
            List<TradePortfolio> tradePortfolios,
            boolean isComplete) {

        // IDs already covered by am-portfolio (de-duplication guard)
        Set<String> coveredPortfolioIds = amPortfolios.stream()
                .map(AnalysisEntity::getSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal dayChange = BigDecimal.ZERO;
        int totalHoldings = 0;

        List<DashboardSummary.PortfolioBreakdown> breakdowns = new ArrayList<>();

        // ── AM Portfolio entities (live holdings) ──────────────────
        for (AnalysisEntity entity : amPortfolios) {
            if (entity.getPerformance() == null)
                continue;

            double calcVal = 0.0;
            double calcInv = 0.0;
            if (entity.getHoldings() != null) {
                for (AnalysisHolding h : entity.getHoldings()) {
                    Double curP = h.getMarket() != null ? h.getMarket().getCurrentPrice() : null;
                    Double avgP = h.getInvestment() != null ? h.getInvestment().getAveragePrice() : null;
                    Double qty  = h.getInvestment() != null ? h.getInvestment().getQuantity() : 1.0;
                    if (qty == null || qty <= 0) qty = 1.0;

                    double itemInv = (avgP != null && avgP > 0) ? (avgP * qty) : 
                            (h.getInvestment() != null && h.getInvestment().getInvestmentValue() != null ? h.getInvestment().getInvestmentValue() : 0.0);
                    
                    double itemVal = (curP != null && curP > 0) ? (curP * qty) :
                            (h.getInvestment() != null && h.getInvestment().getCurrentValue() != null && h.getInvestment().getCurrentValue() > 0 ? h.getInvestment().getCurrentValue() : itemInv);

                    calcVal += itemVal;
                    calcInv += itemInv;
                }
            }

            BigDecimal val = calcVal > 0 ? BigDecimal.valueOf(calcVal) : toBd(entity.getPerformance().getTotalValue());
            BigDecimal inv = calcInv > 0 ? BigDecimal.valueOf(calcInv) : toBd(entity.getPerformance().getTotalInvestment());
            BigDecimal dc = toBd(entity.getPerformance().getDayChange());
            BigDecimal gl = val.subtract(inv);
            double glPct = inv.compareTo(BigDecimal.ONE) >= 0
                    ? gl.divide(inv, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue()
                    : 0.0;

            Double rawDayChangePct = entity.getPerformance().getDayChangePercentage();
            double dayChangePct = rawDayChangePct != null ? rawDayChangePct : 0.0;

            int holdings = entity.getHoldings() != null ? entity.getHoldings().size() : 0;

            totalValue = totalValue.add(val);
            totalInvested = totalInvested.add(inv);
            dayChange = dayChange.add(dc);
            String pid = (entity.getSourceId() != null && !entity.getSourceId().isBlank())
                    ? entity.getSourceId()
                    : "unassigned";
            String pname = (entity.getSourceId() != null && !entity.getSourceId().isBlank())
                    ? entity.getSourceId()
                    : "Unassigned Holdings";

            breakdowns.add(DashboardSummary.PortfolioBreakdown.builder()
                    .portfolioId(pid)
                    .portfolioName(pname)
                    .portfolioType("Long Term")
                    .currentValue(val)
                    .investedValue(inv)
                    .gainLoss(gl)
                    .gainLossPercent(glPct)
                    .dayChange(dc)
                    .dayChangePercent(dayChangePct)
                    .holdingCount(holdings)
                    .build());
        }

        // ── Trade portfolios NOT already covered by am-portfolio ───
        for (TradePortfolio tp : tradePortfolios) {
            // Skip if this trade portfolio is linked to an am-portfolio (avoid
            // double-count)
            if (tp.getExternalPortfolioId() != null && coveredPortfolioIds.contains(tp.getExternalPortfolioId())) {
                log.debug("[Aggregator] Skipping trade portfolio {} — already covered by am-portfolio {}",
                        tp.getId(), tp.getExternalPortfolioId());
                continue;
            }
            BigDecimal val = tp.getTotalValue() != null ? tp.getTotalValue() : BigDecimal.ZERO;
            BigDecimal inv = tp.getTotalInvested() != null ? tp.getTotalInvested() : BigDecimal.ZERO;
            BigDecimal gl = tp.getCurrentPnl() != null ? tp.getCurrentPnl() : BigDecimal.ZERO;

            totalValue = totalValue.add(val);
            totalInvested = totalInvested.add(inv);

            breakdowns.add(DashboardSummary.PortfolioBreakdown.builder()
                    .portfolioId(tp.getId())
                    .portfolioName(tp.getName() != null ? tp.getName() : tp.getType())
                    .portfolioType(tp.getType())
                    .currentValue(val)
                    .investedValue(inv)
                    .gainLoss(gl)
                    .gainLossPercent(tp.getPnlPercentage())
                    .holdingCount(0)
                    .build());
        }

        BigDecimal totalGainLoss = totalValue.subtract(totalInvested);
        double totalGainLossPct = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? totalGainLoss.divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;
        BigDecimal base = totalValue.subtract(dayChange);
        double dayChangePct = base.compareTo(BigDecimal.ZERO) > 0
                ? dayChange.divide(base, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0.0;

        // ── Best / Worst performers (from live holdings) ────────────
        DashboardSummary.PerformerItem best = resolveBestPerformer(amPortfolios);
        DashboardSummary.PerformerItem worst = resolveWorstPerformer(amPortfolios);

        return DashboardSummary.builder()
                .totalValue(totalValue)
                .totalInvested(totalInvested)
                .totalGainLoss(totalGainLoss)
                .totalGainLossPercentage(totalGainLossPct)
                .dayChange(dayChange)
                .dayChangePercentage(dayChangePct)
                .totalPortfolios(breakdowns.size())
                .totalHoldings(totalHoldings)
                .portfolioBreakdown(breakdowns)
                .bestPerformer(best)
                .worstPerformer(worst)
                .currency("INR")
                .isComplete(isComplete)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Portfolio Overviews (one card per portfolio)
    // ─────────────────────────────────────────────────────────────────────

    public List<PortfolioOverview> getPortfolioOverviews(String userId) {
        List<PortfolioOverview> overviews = new ArrayList<>();

        List<AnalysisEntity> amPortfolios = fetchPortfolioEntities(userId);
        Set<String> coveredIds = new HashSet<>();

        if (amPortfolios != null) {
            for (AnalysisEntity entity : amPortfolios) {
                if (entity.getPerformance() == null)
                    continue;
                String pid = entity.getSourceId();
                coveredIds.add(pid);

                int holdingCount = entity.getHoldings() != null ? entity.getHoldings().size() : 0;
                List<String> topSymbols = entity.getHoldings() != null
                        ? entity.getHoldings().stream()
                                .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                                .sorted(Comparator.comparingDouble(
                                        h -> -(h.getInvestment() != null && h.getInvestment().getCurrentValue() != null
                                                ? h.getInvestment().getCurrentValue()
                                                : 0.0)))
                                .limit(3)
                                .map(h -> h.getIdentity().getSymbol())
                                .collect(Collectors.toList())
                        : Collections.emptyList();

                double calcVal = 0.0;
                double calcInv = 0.0;
                if (entity.getHoldings() != null) {
                    for (AnalysisHolding h : entity.getHoldings()) {
                        Double curP = h.getMarket() != null ? h.getMarket().getCurrentPrice() : null;
                        Double avgP = h.getInvestment() != null ? h.getInvestment().getAveragePrice() : null;
                        Double qty = h.getInvestment() != null ? h.getInvestment().getQuantity() : 1.0;
                        if (qty == null || qty <= 0)
                            qty = 1.0;

                        if (curP != null && curP > 0 && avgP != null && avgP > 0) {
                            calcVal += curP * qty;
                            calcInv += avgP * qty;
                        } else if (h.getInvestment() != null) {
                            calcVal += h.getInvestment().getCurrentValue() != null ? h.getInvestment().getCurrentValue()
                                    : 0.0;
                            calcInv += h.getInvestment().getInvestmentValue() != null
                                    ? h.getInvestment().getInvestmentValue()
                                    : 0.0;
                        }
                    }
                }

                BigDecimal val = calcVal > 0 ? BigDecimal.valueOf(calcVal)
                        : toBd(entity.getPerformance().getTotalValue());
                BigDecimal inv = calcInv > 0 ? BigDecimal.valueOf(calcInv)
                        : toBd(entity.getPerformance().getTotalInvestment());
                BigDecimal gl = val.subtract(inv);

                double totalGainLossPct = inv.compareTo(BigDecimal.ZERO) > 0
                        ? gl.doubleValue() / inv.doubleValue() * 100.0
                        : 0.0;

                Double rawDayChangePct = entity.getPerformance().getDayChangePercentage();
                double dayChangePct = rawDayChangePct != null ? rawDayChangePct : 0.0;

                overviews.add(PortfolioOverview.builder()
                        .portfolioId(pid)
                        .portfolioName(pid) // enrich with real name if name stored separately
                        .type("Long Term")
                        .portfolioCount(1)
                        .holdingCount(holdingCount)
                        .totalValue(val)
                        .investedValue(inv)
                        .totalReturn(gl)
                        .returnPercentage(totalGainLossPct)
                        .dayChange(toBd(entity.getPerformance().getDayChange()))
                        .dayChangePercentage(dayChangePct)
                        .topSymbols(topSymbols)
                        .build());
            }
        }

        List<TradePortfolio> tradePortfolios = fetchTradePortfolios(userId);
        if (tradePortfolios != null) {
            for (TradePortfolio tp : tradePortfolios) {
                // Skip trade portfolios already represented by am-portfolio
                if (tp.getExternalPortfolioId() != null && coveredIds.contains(tp.getExternalPortfolioId()))
                    continue;

                BigDecimal val = tp.getTotalValue() != null ? tp.getTotalValue() : BigDecimal.ZERO;
                BigDecimal inv = tp.getTotalInvested() != null ? tp.getTotalInvested() : BigDecimal.ZERO;

                overviews.add(PortfolioOverview.builder()
                        .portfolioId(tp.getId())
                        .portfolioName(tp.getName() != null ? tp.getName() : tp.getType())
                        .type(tp.getType() != null ? tp.getType() : "Trade")
                        .portfolioCount(1)
                        .holdingCount(0)
                        .totalValue(val)
                        .investedValue(inv)
                        .totalReturn(tp.getCurrentPnl())
                        .returnPercentage(tp.getPnlPercentage())
                        .topSymbols(Collections.emptyList())
                        .build());
            }
        }

        return overviews;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Best / Worst Performer helpers
    // ─────────────────────────────────────────────────────────────────────

    private DashboardSummary.PerformerItem resolveBestPerformer(List<AnalysisEntity> entities) {
        return entities.stream()
                .flatMap(e -> e.getHoldings() != null ? e.getHoldings().stream() : java.util.stream.Stream.empty())
                .filter(h -> h.getMarket() != null && h.getMarket().getDayChangePercentage() != null)
                .max(Comparator.comparingDouble(h -> h.getMarket().getDayChangePercentage()))
                .map(this::toPerformerItem)
                .orElse(null);
    }

    private DashboardSummary.PerformerItem resolveWorstPerformer(List<AnalysisEntity> entities) {
        return entities.stream()
                .flatMap(e -> e.getHoldings() != null ? e.getHoldings().stream() : java.util.stream.Stream.empty())
                .filter(h -> h.getMarket() != null && h.getMarket().getDayChangePercentage() != null)
                .min(Comparator.comparingDouble(h -> h.getMarket().getDayChangePercentage()))
                .map(this::toPerformerItem)
                .orElse(null);
    }

    private DashboardSummary.PerformerItem toPerformerItem(AnalysisHolding h) {
        return DashboardSummary.PerformerItem.builder()
                .symbol(h.getIdentity() != null ? h.getIdentity().getSymbol() : null)
                .companyName(h.getIdentity() != null ? h.getIdentity().getCompanyName() : null)
                .changePercent(h.getMarket() != null ? h.getMarket().getDayChangePercentage() : null)
                .profitLossPercent(h.getInvestment() != null ? h.getInvestment().getProfitLossPercentage() : null)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Circuit-breaker-protected fetches
    // ─────────────────────────────────────────────────────────────────────

    @CircuitBreaker(name = "portfolioService", fallbackMethod = "portfolioFallback")
    @Retry(name = "portfolioService")
    List<AnalysisEntity> fetchPortfolioEntities(String userId) {
        long start = System.nanoTime();
        EntityLoadResult result = entityLoadService.loadPortfoliosForUser(userId, BootstrapTrigger.DASHBOARD);
        List<AnalysisEntity> entities = result.entities();
        flowLogger.step("analysis.aggregator.fetch_portfolios",
                "userId", userId,
                "entities", entities.size(),
                "bootstrap_requested", result.bootstrapRequested(),
                "duration_ms", (System.nanoTime() - start) / 1_000_000L);
        return entities;
    }

    List<AnalysisEntity> portfolioFallback(String userId, Throwable ex) {
        flowLogger.step("analysis.aggregator.fallback.portfolio",
                "userId", userId,
                "cause", ex.getClass().getSimpleName(),
                "cause.message", ex.getMessage());
        return null;
    }

    @CircuitBreaker(name = "tradeService", fallbackMethod = "tradeFallback")
    @Retry(name = "tradeService")
    List<TradePortfolio> fetchTradePortfolios(String userId) {
        long start = System.nanoTime();
        List<TradePortfolio> trades = tradeClientService.getPortfolios(userId);
        flowLogger.step("analysis.aggregator.fetch_trades",
                "userId", userId,
                "trades", trades == null ? 0 : trades.size(),
                "duration_ms", (System.nanoTime() - start) / 1_000_000L);
        return trades;
    }

    List<TradePortfolio> tradeFallback(String userId, Throwable ex) {
        flowLogger.step("analysis.aggregator.fallback.trade",
                "userId", userId,
                "cause", ex.getClass().getSimpleName(),
                "cause.message", ex.getMessage());
        return null;
    }

    /**
     * Null-safe Double → BigDecimal (Mongo performance fields are often partially
     * populated).
     */
    private static BigDecimal toBd(Double value) {
        return value != null ? BigDecimal.valueOf(value) : BigDecimal.ZERO;
    }
}
