package com.am.mcp.util;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates analysis HOLDING entities into portfolio summary / holdings list shapes
 * expected by fin-agent widgets (investmentValue, currentValue, totalAssets, …).
 */
public final class PortfolioAnalysisAggregator {

    private PortfolioAnalysisAggregator() {
    }

    public static List<AnalysisEntity> filterByPortfolioId(List<AnalysisEntity> entities, String portfolioId) {
        if (portfolioId == null || portfolioId.isBlank()) {
            return entities;
        }
        return entities.stream()
                .filter(e -> portfolioId.equals(e.getSourceId()))
                .collect(Collectors.toList());
    }

    public static Map<String, Object> summarize(List<AnalysisEntity> entities) {
        double invested = 0;
        double current = 0;
        double todayGain = 0;
        int holdingsCount = 0;
        int gainers = 0;
        int losers = 0;

        for (AnalysisHolding h : flattenHoldings(entities)) {
            InvestmentStats inv = h.getInvestment();
            if (inv != null) {
                if (inv.getInvestmentValue() != null) {
                    invested += inv.getInvestmentValue();
                }
                if (inv.getCurrentValue() != null) {
                    current += inv.getCurrentValue();
                }
                Double plPct = inv.getProfitLossPercentage();
                if (plPct != null) {
                    if (plPct > 0) {
                        gainers++;
                    } else if (plPct < 0) {
                        losers++;
                    }
                }
            }
            MarketStats market = h.getMarket();
            if (market != null && market.getDayChange() != null) {
                double qty = inv != null && inv.getQuantity() != null ? inv.getQuantity() : 1.0;
                todayGain += market.getDayChange() * qty;
            }
            holdingsCount++;
        }

        // Entity-level performance day change when nested holdings lack market.dayChange
        if (todayGain == 0) {
            for (AnalysisEntity e : entities) {
                PerformanceSummary perf = e.getPerformance();
                if (perf != null && perf.getDayChange() != null) {
                    todayGain += perf.getDayChange();
                }
            }
        }

        double pnl = current - invested;
        double pnlPct = invested > 0 ? round2((pnl / invested) * 100.0) : 0.0;
        double todayPct = current > 0 ? round2((todayGain / current) * 100.0) : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("investmentValue", round2(invested));
        result.put("currentValue", round2(current));
        result.put("totalValue", round2(current));
        result.put("totalInvested", round2(invested));
        result.put("totalGainLoss", round2(pnl));
        result.put("totalGainLossPercentage", pnlPct);
        result.put("todayGainLoss", round2(todayGain));
        result.put("todayGainLossPercentage", todayPct);
        result.put("dayChange", round2(todayGain));
        result.put("dayChangePercentage", todayPct);
        result.put("totalAssets", holdingsCount);
        result.put("totalHoldings", holdingsCount);
        result.put("totalPortfolios", entities.isEmpty() ? 0 : 1);
        result.put("gainersCount", gainers);
        result.put("losersCount", losers);
        result.put("currency", "INR");
        return result;
    }

    public static List<Map<String, Object>> listHoldings(List<AnalysisEntity> entities) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AnalysisHolding h : flattenHoldings(entities)) {
            Map<String, Object> row = new LinkedHashMap<>();
            if (h.getIdentity() != null) {
                row.put("symbol", h.getIdentity().getSymbol());
                row.put("name", firstNonBlank(h.getIdentity().getName(), h.getIdentity().getCompanyName()));
                row.put("assetClass", h.getIdentity().getAssetClass());
                row.put("exchange", h.getIdentity().getExchange());
            }
            InvestmentStats inv = h.getInvestment();
            if (inv != null) {
                row.put("quantity", inv.getQuantity());
                row.put("averagePrice", inv.getAveragePrice());
                row.put("investmentValue", inv.getInvestmentValue());
                row.put("currentValue", inv.getCurrentValue());
                row.put("profitLoss", inv.getProfitLoss());
                row.put("profitLossPercentage", inv.getProfitLossPercentage());
            }
            MarketStats market = h.getMarket();
            if (market != null) {
                row.put("dayChange", market.getDayChange());
                row.put("dayChangePercentage", market.getDayChangePercentage());
                row.put("currentPrice", market.getCurrentPrice());
            }
            if (h.getClassification() != null) {
                row.put("sector", h.getClassification().getSector());
                row.put("marketCapType", h.getClassification().getMarketCapType());
            }
            rows.add(row);
        }
        return rows;
    }

    static List<AnalysisHolding> flattenHoldings(List<AnalysisEntity> entities) {
        List<AnalysisHolding> out = new ArrayList<>();
        for (AnalysisEntity e : entities) {
            if (e.getHoldings() != null && !e.getHoldings().isEmpty()) {
                out.addAll(e.getHoldings());
                continue;
            }
            // Per-symbol HOLDING documents: treat entity performance as one holding
            if (e.getPerformance() != null || e.getSourceId() != null) {
                AnalysisHolding synthetic = new AnalysisHolding();
                var identity = new com.am.analysis.adapter.model.components.HoldingIdentity();
                identity.setSymbol(e.getSourceId());
                synthetic.setIdentity(identity);
                PerformanceSummary perf = e.getPerformance();
                if (perf != null) {
                    InvestmentStats inv = new InvestmentStats();
                    inv.setInvestmentValue(perf.getTotalInvestment());
                    inv.setCurrentValue(perf.getTotalValue());
                    inv.setProfitLoss(perf.getTotalGainLoss());
                    inv.setProfitLossPercentage(perf.getTotalGainLossPercentage());
                    synthetic.setInvestment(inv);
                    MarketStats market = new MarketStats();
                    market.setDayChange(perf.getDayChange());
                    market.setDayChangePercentage(perf.getDayChangePercentage());
                    synthetic.setMarket(market);
                }
                out.add(synthetic);
            }
        }
        return out;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }
}
