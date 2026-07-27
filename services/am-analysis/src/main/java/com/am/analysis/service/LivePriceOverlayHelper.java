package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.kafka.service.GainLossCalculator;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Applies Kafka tick prices onto in-memory {@link AnalysisEntity} graphs for streaming publish.
 * Resolves symbol aliases (NSE:ITC, NSE|ITC → ITC).
 */
public final class LivePriceOverlayHelper {

    /** NSE/BSE standard circuit — daily % above this implies bad prev-close data. */
    public static final double MAX_PLAUSIBLE_DAILY_MOVE_PCT = 20.0;

    private LivePriceOverlayHelper() {
    }

    /** Infer average buy when Mongo holdings omit averagePrice. */
    public static double inferAveragePrice(InvestmentStats inv) {
        if (inv == null) {
            return 0.0;
        }
        double avgBuy = inv.getAveragePrice() != null ? inv.getAveragePrice() : 0.0;
        if (avgBuy > 0) {
            return avgBuy;
        }
        double qty = inv.getQuantity() != null ? inv.getQuantity() : 0.0;
        double investmentValue = inv.getInvestmentValue() != null ? inv.getInvestmentValue() : 0.0;
        if (qty > 0 && investmentValue > 0) {
            return investmentValue / qty;
        }
        return 0.0;
    }

    public static boolean isPlausibleDailyMove(double price, double prevClose) {
        if (price <= 0 || prevClose <= 0) {
            return false;
        }
        double pct = Math.abs((price - prevClose) / prevClose) * 100.0;
        return pct <= MAX_PLAUSIBLE_DAILY_MOVE_PCT;
    }

    /** Per-share daily % from last price and previous close. */
    public static Double computeDailyChangePercent(double price, double prevClose) {
        if (price <= 0 || prevClose <= 0 || !isPlausibleDailyMove(price, prevClose)) {
            return null;
        }
        return ((price - prevClose) / prevClose) * 100.0;
    }

    public static Double computeDailyChangeAmount(double qty, double price, double prevClose) {
        if (qty <= 0 || price <= 0 || prevClose <= 0 || !isPlausibleDailyMove(price, prevClose)) {
            return null;
        }
        return GainLossCalculator.todayGainLoss(qty, price, prevClose);
    }

    public static Double resolvePlausiblePrevClose(double price, Double tickPrevClose, Double storedPrevClose) {
        if (tickPrevClose != null && tickPrevClose > 0 && isPlausibleDailyMove(price, tickPrevClose)) {
            return tickPrevClose;
        }
        if (storedPrevClose != null && storedPrevClose > 0 && isPlausibleDailyMove(price, storedPrevClose)) {
            return storedPrevClose;
        }
        return null;
    }

    /** Rejects corrupt reference prices; caps vary by window. */
    public static boolean isValidPeriodReference(double price, double referencePrice,
                                                 com.am.kafka.config.Timeframe window) {
        if (price <= 0 || referencePrice <= 0) {
            return false;
        }
        double pct = Math.abs((price - referencePrice) / referencePrice) * 100.0;
        return pct <= maxPeriodMovePercent(window);
    }

    static double maxPeriodMovePercent(com.am.kafka.config.Timeframe window) {
        if (window == null) {
            return 100.0;
        }
        return switch (window) {
            case ONE_WEEK -> 50.0;
            case ONE_MONTH -> 100.0;
            case THREE_MONTHS -> 150.0;
            case SIX_MONTHS -> 200.0;
            case ONE_YEAR, FIVE_YEARS -> 500.0;
            default -> 100.0;
        };
    }

    public static Double resolvePeriodReferencePrice(double price, Double tickReference, Double storedReference,
                                                     com.am.kafka.config.Timeframe window) {
        if (tickReference != null && tickReference > 0 && isValidPeriodReference(price, tickReference, window)) {
            return tickReference;
        }
        if (storedReference != null && storedReference > 0 && isValidPeriodReference(price, storedReference, window)) {
            return storedReference;
        }
        return null;
    }

    /** Period return % for 1W / 1M / 1Y — window-specific sanity cap, no intraday circuit. */
    public static Double computePeriodChangePercent(double price, double referencePrice,
                                                    com.am.kafka.config.Timeframe window) {
        if (!isValidPeriodReference(price, referencePrice, window)) {
            return null;
        }
        return ((price - referencePrice) / referencePrice) * 100.0;
    }

    public static Double computePeriodChangeAmount(double qty, double price, double referencePrice,
                                                   com.am.kafka.config.Timeframe window) {
        if (qty <= 0 || !isValidPeriodReference(price, referencePrice, window)) {
            return null;
        }
        return GainLossCalculator.todayGainLoss(qty, price, referencePrice);
    }

    public static void applyAll(List<AnalysisEntity> entities, Map<String, LivePriceTick> ticks) {
        applyAll(entities, ticks, com.am.kafka.config.Timeframe.ONE_DAY);
    }

    public static void applyAll(List<AnalysisEntity> entities, Map<String, LivePriceTick> ticks,
                                com.am.kafka.config.Timeframe window) {
        if (entities == null || ticks == null || ticks.isEmpty()) {
            return;
        }
        for (AnalysisEntity entity : entities) {
            apply(entity, ticks, window);
        }
    }

    public static void apply(AnalysisEntity entity, Map<String, LivePriceTick> ticks) {
        apply(entity, ticks, com.am.kafka.config.Timeframe.ONE_DAY);
    }

    public static void apply(AnalysisEntity entity, Map<String, LivePriceTick> ticks,
                             com.am.kafka.config.Timeframe window) {
        if (entity == null || entity.getHoldings() == null || entity.getHoldings().isEmpty()
                || ticks == null || ticks.isEmpty()) {
            return;
        }

        boolean intraday = window == null || window.isIntraday();
        double totalValue = 0.0;
        double totalInvestment = 0.0;
        double totalGainLoss = 0.0;
        double todayGainLoss = 0.0;

        for (AnalysisHolding holding : entity.getHoldings()) {
            recalculateHolding(holding, ticks, window);

            InvestmentStats inv = holding.getInvestment();
            MarketStats market = holding.getMarket();
            if (inv == null) {
                continue;
            }

            totalValue += inv.getCurrentValue() != null ? inv.getCurrentValue() : 0.0;
            totalInvestment += inv.getInvestmentValue() != null ? inv.getInvestmentValue() : 0.0;
            totalGainLoss += inv.getProfitLoss() != null ? inv.getProfitLoss() : 0.0;
            if (intraday && market != null && market.getDayChange() != null) {
                todayGainLoss += market.getDayChange();
            }
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
        perf.setDayChangePercentage(priorDayValue > 0 && Math.abs(todayGainLoss) > 0
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

    /** Expands holding symbols to Redis prev-close key variants (e.g. SAIL → NSE_EQ:SAIL). */
    public static Set<String> expandRedisSymbolKeys(Collection<String> holdingSymbols) {
        Set<String> keys = new LinkedHashSet<>();
        if (holdingSymbols == null) {
            return keys;
        }
        for (String symbol : holdingSymbols) {
            if (symbol == null || symbol.isBlank()) {
                continue;
            }
            String trimmed = symbol.trim();
            keys.add(trimmed);
            String base = baseSymbol(trimmed);
            if (!base.isEmpty()) {
                keys.add(base);
                keys.add("NSE_EQ:" + base);
                keys.add("NSE:" + base);
            }
        }
        return keys;
    }

    /** Resolves prev-close for a holding symbol from a Redis batch result (exact key, then base-symbol match). */
    public static Double resolvePrevClose(String holdingSymbol, Map<String, Double> prevCloseByRedisKey) {
        if (holdingSymbol == null || prevCloseByRedisKey == null || prevCloseByRedisKey.isEmpty()) {
            return null;
        }
        Double exact = prevCloseByRedisKey.get(holdingSymbol);
        if (exact != null) {
            return exact;
        }
        String base = baseSymbol(holdingSymbol);
        if (!base.isEmpty()) {
            Double baseHit = prevCloseByRedisKey.get(base);
            if (baseHit != null) {
                return baseHit;
            }
        }
        for (Map.Entry<String, Double> entry : prevCloseByRedisKey.entrySet()) {
            if (baseSymbol(entry.getKey()).equalsIgnoreCase(base)) {
                return entry.getValue();
            }
        }
        return null;
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

    private static void recalculateHolding(AnalysisHolding holding, Map<String, LivePriceTick> ticks,
                                           com.am.kafka.config.Timeframe window) {
        if (holding.getIdentity() == null || holding.getInvestment() == null) {
            return;
        }

        boolean intraday = window == null || window.isIntraday();
        String symbol = holding.getIdentity().getSymbol();
        InvestmentStats inv = holding.getInvestment();
        MarketStats market = holding.getMarket();
        if (market == null) {
            market = new MarketStats();
            holding.setMarket(market);
        }

        double qty = inv.getQuantity() != null ? inv.getQuantity() : 0.0;
        double avgBuy = inferAveragePrice(inv);
        double investmentValue = inv.getInvestmentValue() != null ? inv.getInvestmentValue() : 0.0;
        if (investmentValue <= 0 && avgBuy > 0 && qty > 0) {
            investmentValue = avgBuy * qty;
            inv.setInvestmentValue(investmentValue);
        }

        double price = market.getCurrentPrice() != null && market.getCurrentPrice() > 0 ? market.getCurrentPrice() : 0.0;
        Double tickReference = null;
        LivePriceTick tick = resolveTick(symbol, ticks);
        if (tick != null && tick.lastPrice() > 0) {
            price = tick.lastPrice();
            market.setCurrentPrice(price);
            tickReference = tick.previousClose();
        } else if (price <= 0 && avgBuy > 0) {
            price = avgBuy;
            market.setCurrentPrice(price);
        }

        Double referencePrice = intraday
                ? resolvePlausiblePrevClose(price, tickReference, market.getPreviousClose())
                : resolvePeriodReferencePrice(price, tickReference, market.getPreviousClose(), window);

        Double existingDayChange = market.getDayChange();
        Double existingDayChangePct = market.getDayChangePercentage();

        if (referencePrice != null) {
            market.setPreviousClose(referencePrice);
        } else {
            market.setPreviousClose(null);
        }

        double currentValue = qty > 0
                ? GainLossCalculator.currentValue(qty, price)
                : (inv.getValue() != null && inv.getValue() > 0 ? inv.getValue() : 0.0);
        double profitLoss = avgBuy > 0
                ? GainLossCalculator.totalGainLoss(qty, price, avgBuy)
                : (investmentValue > 0 ? currentValue - investmentValue
                    : (inv.getProfitLoss() != null ? inv.getProfitLoss() : 0.0));

        Double periodChange = referencePrice != null
                ? (intraday
                    ? computeDailyChangeAmount(qty, price, referencePrice)
                    : computePeriodChangeAmount(qty, price, referencePrice, window))
                : (existingDayChange != null ? existingDayChange : 0.0);
        Double periodChangePct = referencePrice != null
                ? (intraday
                    ? computeDailyChangePercent(price, referencePrice)
                    : computePeriodChangePercent(price, referencePrice, window))
                : (existingDayChangePct != null ? existingDayChangePct : 0.0);

        inv.setCurrentValue(currentValue);
        inv.setProfitLoss(profitLoss);
        inv.setProfitLossPercentage(investmentValue > 0
                ? GainLossCalculator.gainLossPercent(profitLoss, investmentValue)
                : 0.0);
        inv.setValue(currentValue);
        if (avgBuy > 0) {
            inv.setAveragePrice(avgBuy);
        }

        market.setDayChange(periodChange);
        market.setDayChangePercentage(periodChangePct);
    }
}
