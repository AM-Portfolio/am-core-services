package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.kafka.service.GainLossCalculator;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Applies Kafka tick prices onto in-memory {@link AnalysisEntity} graphs for streaming publish.
 * Resolves symbol aliases (NSE:ITC, NSE|ITC → ITC).
 */
public final class LivePriceOverlayHelper {

    private LivePriceOverlayHelper() {
    }

    public static void applyAll(List<AnalysisEntity> entities, Map<String, LivePriceTick> ticks) {
        if (entities == null || ticks == null || ticks.isEmpty()) {
            return;
        }
        for (AnalysisEntity entity : entities) {
            apply(entity, ticks);
        }
    }

    public static void apply(AnalysisEntity entity, Map<String, LivePriceTick> ticks) {
        if (entity == null || entity.getHoldings() == null || entity.getHoldings().isEmpty()
                || ticks == null || ticks.isEmpty()) {
            return;
        }

        double totalValue = 0.0;
        double totalInvestment = 0.0;
        double totalGainLoss = 0.0;
        double todayGainLoss = 0.0;

        for (AnalysisHolding holding : entity.getHoldings()) {
            recalculateHolding(holding, ticks);

            InvestmentStats inv = holding.getInvestment();
            MarketStats market = holding.getMarket();
            if (inv == null) {
                continue;
            }

            totalValue += inv.getCurrentValue() != null ? inv.getCurrentValue() : 0.0;
            totalInvestment += inv.getInvestmentValue() != null ? inv.getInvestmentValue() : 0.0;
            totalGainLoss += inv.getProfitLoss() != null ? inv.getProfitLoss() : 0.0;
            todayGainLoss += market != null && market.getDayChange() != null ? market.getDayChange() : 0.0;
        }

        PerformanceSummary perf = entity.getPerformance();
        if (perf == null) {
            perf = new PerformanceSummary();
            entity.setPerformance(perf);
        }
        perf.setTotalValue(totalValue);
        perf.setTotalInvestment(totalInvestment);
        perf.setTotalGainLoss(totalGainLoss);
        perf.setTotalGainLossPercentage(GainLossCalculator.gainLossPercent(totalGainLoss, totalInvestment));
        perf.setDayChange(todayGainLoss);
        double priorDayValue = totalValue - todayGainLoss;
        perf.setDayChangePercentage(priorDayValue > 0
                ? GainLossCalculator.gainLossPercent(todayGainLoss, priorDayValue)
                : 0.0);
    }

    public static LivePriceTick resolveTick(String holdingSymbol, Map<String, LivePriceTick> ticks) {
        if (holdingSymbol == null || ticks == null || ticks.isEmpty()) {
            return null;
        }
        if (ticks.containsKey(holdingSymbol)) {
            return ticks.get(holdingSymbol);
        }

        String base = baseSymbol(holdingSymbol);
        if (ticks.containsKey(base)) {
            return ticks.get(base);
        }

        for (Map.Entry<String, LivePriceTick> entry : ticks.entrySet()) {
            if (baseSymbol(entry.getKey()).equalsIgnoreCase(base)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String baseSymbol(String symbol) {
        if (symbol == null) {
            return "";
        }
        String normalized = symbol.trim();
        if (normalized.contains("|")) {
            normalized = normalized.substring(normalized.lastIndexOf('|') + 1);
        }
        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.lastIndexOf(':') + 1);
        }
        return normalized.trim();
    }

    public static Map<String, LivePriceTick> indexByBaseSymbol(Map<String, LivePriceTick> ticks) {
        Map<String, LivePriceTick> indexed = new HashMap<>();
        if (ticks == null) {
            return indexed;
        }
        for (Map.Entry<String, LivePriceTick> entry : ticks.entrySet()) {
            indexed.putIfAbsent(baseSymbol(entry.getKey()).toUpperCase(Locale.ROOT), entry.getValue());
        }
        return indexed;
    }

    private static void recalculateHolding(AnalysisHolding holding, Map<String, LivePriceTick> ticks) {
        if (holding.getIdentity() == null || holding.getInvestment() == null) {
            return;
        }

        String symbol = holding.getIdentity().getSymbol();
        InvestmentStats inv = holding.getInvestment();
        MarketStats market = holding.getMarket();
        if (market == null) {
            market = new MarketStats();
            holding.setMarket(market);
        }

        double qty = inv.getQuantity() != null ? inv.getQuantity() : 0.0;
        double avgBuy = inv.getAveragePrice() != null ? inv.getAveragePrice() : 0.0;
        double investmentValue = inv.getInvestmentValue() != null ? inv.getInvestmentValue() : 0.0;

        double price = market.getCurrentPrice() != null ? market.getCurrentPrice() : 0.0;
        LivePriceTick tick = resolveTick(symbol, ticks);
        if (tick != null) {
            price = tick.lastPrice();
            market.setCurrentPrice(price);
            if (tick.previousClose() != null && tick.previousClose() > 0
                    && (market.getPreviousClose() == null || market.getPreviousClose() <= 0)) {
                market.setPreviousClose(tick.previousClose());
            }
        }

        double prevClose = market.getPreviousClose() != null && market.getPreviousClose() > 0
                ? market.getPreviousClose()
                : price;
        double currentValue = GainLossCalculator.currentValue(qty, price);
        double profitLoss = GainLossCalculator.totalGainLoss(qty, price, avgBuy);
        double dayChange = GainLossCalculator.todayGainLoss(qty, price, prevClose);

        inv.setCurrentValue(currentValue);
        inv.setProfitLoss(profitLoss);
        inv.setProfitLossPercentage(GainLossCalculator.gainLossPercent(profitLoss, investmentValue));
        inv.setValue(currentValue);

        market.setDayChange(dayChange);
        double priorCloseValue = qty * prevClose;
        market.setDayChangePercentage(priorCloseValue > 0
                ? GainLossCalculator.gainLossPercent(dayChange, priorCloseValue)
                : 0.0);
    }
}
