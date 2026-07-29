package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.LivePriceOverlayHelper;
import com.am.analysis.service.LivePriceTick;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.BootstrapTrigger;
import com.am.analysis.service.load.EntityLoadRequest;
import com.am.analysis.service.load.EntityLoadResult;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import com.am.kafka.config.Timeframe;
import com.am.kafka.service.PreviousCloseRedisService;
import com.am.market.client.service.MarketDataClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopMoversAnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisEntityLoadService entityLoadService;
    private final AnalysisAccessValidator accessValidator;
    private final PreviousCloseRedisService previousCloseRedisService;
    private final MarketDataClientService marketDataClientService;

    public TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId, AnalysisGroupBy groupBy) {
        return getTopMovers(id, type, timeFrame, userId, groupBy, Map.of());
    }

    public TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId,
                                          AnalysisGroupBy groupBy, Map<String, LivePriceTick> liveTicks) {
        if (id == null) {
            log.info("Processing Top Movers by Category: Type={}, TimeFrame={}, User={}, GroupBy={}", type, timeFrame, userId, groupBy);
            return getTopMoversByCategory(type, timeFrame, userId, groupBy, liveTicks);
        } else {
            log.info("Processing Top Movers within Entity: ID={}, Type={}, TimeFrame={}, User={}, GroupBy={}", id, type, timeFrame, userId, groupBy);
            return getTopMoversWithinEntity(id, type, timeFrame, userId, groupBy, liveTicks);
        }
    }

    private TopMoversResponse getTopMoversByCategory(AnalysisEntityType type, String timeFrame, String userId,
                                                     AnalysisGroupBy groupBy, Map<String, LivePriceTick> liveTicks) {
        if (type == AnalysisEntityType.PORTFOLIO && userId != null) {
            log.debug("Aggregating portfolio holdings for user: {}", userId);
            EntityLoadResult loadResult = entityLoadService.loadPortfoliosForUser(userId, BootstrapTrigger.HTTP_READ);
            List<AnalysisEntity> portfolios = loadResult.entities();

            if (portfolios.isEmpty()) {
                return emptyTopMovers(timeFrame);
            }

            applyLiveMarketData(portfolios, liveTicks, timeFrame);
            
            List<com.am.analysis.adapter.model.AnalysisHolding> allHoldings = portfolios.stream()
                .filter(p -> p.getHoldings() != null)
                .flatMap(p -> p.getHoldings().stream())
                .collect(java.util.stream.Collectors.toList());

            double totalPortfolioValue = portfolios.stream()
                .filter(p -> p.getPerformance() != null && p.getPerformance().getTotalValue() != null)
                .mapToDouble(p -> p.getPerformance().getTotalValue())
                .sum();

            if (groupBy != null && groupBy != AnalysisGroupBy.STOCK) {
                return getTopMoversByGroup(allHoldings, timeFrame, totalPortfolioValue, groupBy);
            }
            
            log.debug("Found {} total holdings from {} portfolios", allHoldings.size(), portfolios.size());

            // Deduplicate by symbol for STOCK view
            Map<String, com.am.analysis.adapter.model.AnalysisHolding> uniqueHoldings = allHoldings.stream()
                .filter(h -> resolveSymbol(h) != null)
                .collect(java.util.stream.Collectors.toMap(
                    h -> resolveSymbol(h),
                    h -> h,
                    (existing, replacement) -> existing // Keep existing
                ));
            
            List<com.am.analysis.adapter.model.AnalysisHolding> holdings = new java.util.ArrayList<>(uniqueHoldings.values());
            
            String normalizedTf = normalizeTimeFrame(timeFrame);

            List<com.am.analysis.adapter.model.AnalysisHolding> gainers = holdings.stream()
                    .filter(h -> resolveChangeMetric(h, normalizedTf) > 0)
                    .sorted((h1, h2) -> Double.compare(resolveChangeMetric(h2, normalizedTf), resolveChangeMetric(h1, normalizedTf)))
                    .limit(10)
                    .toList();
            
            List<com.am.analysis.adapter.model.AnalysisHolding> losers = holdings.stream()
                    .filter(h -> resolveChangeMetric(h, normalizedTf) < 0)
                    .sorted((h1, h2) -> Double.compare(resolveChangeMetric(h1, normalizedTf), resolveChangeMetric(h2, normalizedTf)))
                    .limit(10)
                    .toList();

            if (gainers.isEmpty() && losers.isEmpty()) {
                long withChange = holdings.stream()
                        .filter(h -> computeChangePercentage(h, normalizedTf) != null)
                        .count();
                log.warn("[TopMovers] Empty gainers/losers for userId={} tf={}: portfolios={}, holdings={}, withChangePct={}",
                        userId, normalizedTf, portfolios.size(), holdings.size(), withChange);
            }

            return buildTopMoversResponseFromHoldings(gainers, losers, normalizedTf, totalPortfolioValue);
        }

        // Fallback for non-portfolio types or public types (if any)
        List<AnalysisEntity> gainers = repository.findTop10ByTypeOrderByPerformanceTotalGainLossPercentageDesc(type);
        List<AnalysisEntity> losers = repository.findTop10ByTypeOrderByPerformanceTotalGainLossPercentageAsc(type);
        return buildTopMoversResponse(gainers, losers);
    }

    private TopMoversResponse getTopMoversWithinEntity(String id, AnalysisEntityType type, String timeFrame,
                                                       String userId, AnalysisGroupBy groupBy,
                                                       Map<String, LivePriceTick> liveTicks) {
        if (type == AnalysisEntityType.PORTFOLIO) {
            EntityLoadResult loadResult = entityLoadService.loadOne(
                    EntityLoadRequest.onePortfolio(id, userId, BootstrapTrigger.HTTP_READ));
            if (loadResult.empty() || loadResult.entities().isEmpty()) {
                return emptyTopMovers(timeFrame);
            }
            return buildTopMoversForEntity(loadResult.entities().get(0), timeFrame, groupBy, liveTicks);
        }

        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            accessValidator.verifyAccess(entityOpt.get(), userId);
            return buildTopMoversForEntity(entityOpt.get(), timeFrame, groupBy, liveTicks);
        }

        return emptyTopMovers(timeFrame);
    }

    private TopMoversResponse buildTopMoversForEntity(AnalysisEntity entity, String timeFrame,
                                                      AnalysisGroupBy groupBy, Map<String, LivePriceTick> liveTicks) {
        if (entity.getHoldings() == null) {
            return emptyTopMovers(timeFrame);
        }

        applyLiveMarketData(List.of(entity), liveTicks, timeFrame);

        double totalPortfolioValue = entity.getHoldings().stream()
                .mapToDouble(h -> (h.getInvestment() != null && h.getInvestment().getValue() != null)
                        ? h.getInvestment().getValue() : 0.0)
                .sum();

        if (groupBy != null && groupBy != AnalysisGroupBy.STOCK) {
            return getTopMoversByGroup(entity.getHoldings(), timeFrame, totalPortfolioValue, groupBy);
        }

        List<AnalysisHolding> holdings = entity.getHoldings();
        String normalizedTf = normalizeTimeFrame(timeFrame);

        List<AnalysisHolding> gainers = holdings.stream()
                .filter(h -> resolveChangeMetric(h, normalizedTf) > 0)
                .sorted((h1, h2) -> Double.compare(resolveChangeMetric(h2, normalizedTf), resolveChangeMetric(h1, normalizedTf)))
                .limit(10)
                .toList();

        List<AnalysisHolding> losers = holdings.stream()
                .filter(h -> resolveChangeMetric(h, normalizedTf) < 0)
                .sorted((h1, h2) -> Double.compare(resolveChangeMetric(h1, normalizedTf), resolveChangeMetric(h2, normalizedTf)))
                .limit(10)
                .toList();

        return buildTopMoversResponseFromHoldings(gainers, losers, normalizedTf, totalPortfolioValue);
    }

    private TopMoversResponse emptyTopMovers(String timeFrame) {
        return TopMoversResponse.builder()
                .gainers(List.of())
                .losers(List.of())
                .timeFrame(timeFrame != null ? timeFrame : "1D")
                .build();
    }

    private void applyLiveMarketData(List<AnalysisEntity> portfolios, Map<String, LivePriceTick> liveTicks,
                                     String timeFrame) {
        Timeframe window = resolveTimeframe(timeFrame);
        if (liveTicks != null && !liveTicks.isEmpty()) {
            LivePriceOverlayHelper.applyAll(portfolios, liveTicks, window);
            return;
        }

        List<String> holdingSymbols = portfolios.stream()
                .filter(p -> p.getHoldings() != null)
                .flatMap(p -> p.getHoldings().stream())
                .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                .map(h -> h.getIdentity().getSymbol())
                .distinct()
                .toList();
        if (holdingSymbols.isEmpty()) {
            return;
        }

        Set<String> redisKeys = LivePriceOverlayHelper.expandRedisSymbolKeys(holdingSymbols);
        Map<String, Double> prevCloseByRedisKey =
                previousCloseRedisService.readWindowForSymbols(redisKeys, window);
        if (prevCloseByRedisKey.isEmpty()) {
            log.warn("[TopMovers] No reference price in Redis for {} symbols (window={})",
                    holdingSymbols.size(), window.getCode());
        }

        Map<String, LivePriceTick> ticks = buildTicksFromHoldingsAndPrevClose(
                portfolios, holdingSymbols, prevCloseByRedisKey);
        if (ticks.isEmpty()) {
            log.warn("[TopMovers] No ticks built: missing currentPrice on holdings for {} symbols",
                    holdingSymbols.size());
            return;
        }
        LivePriceOverlayHelper.applyAll(portfolios, ticks, window);
    }

    private static Timeframe resolveTimeframe(String timeFrame) {
        return Timeframe.tryFromCode(timeFrame != null ? timeFrame : "1D")
                .orElse(Timeframe.ONE_DAY);
    }

    private static String normalizeTimeFrame(String timeFrame) {
        return resolveTimeframe(timeFrame).getCode();
    }

    private static boolean isDailyTimeFrame(String timeFrame) {
        return resolveTimeframe(timeFrame).isIntraday();
    }

    private Map<String, LivePriceTick> buildTicksFromHoldingsAndPrevClose(
            List<AnalysisEntity> portfolios,
            List<String> holdingSymbols,
            Map<String, Double> prevCloseByRedisKey) {
        Map<String, LivePriceTick> ticks = new HashMap<>();
        for (String symbol : holdingSymbols) {
            Double lastPrice = findCurrentPrice(portfolios, symbol);
            if (lastPrice == null || lastPrice <= 0) {
                continue;
            }
            Double prevClose = LivePriceOverlayHelper.resolvePrevClose(symbol, prevCloseByRedisKey);
            ticks.put(symbol, new LivePriceTick(lastPrice, prevClose));
        }
        return ticks;
    }

    private static Double findCurrentPrice(List<AnalysisEntity> portfolios, String symbol) {
        for (AnalysisEntity portfolio : portfolios) {
            if (portfolio.getHoldings() == null) {
                continue;
            }
            for (AnalysisHolding holding : portfolio.getHoldings()) {
                if (holding.getIdentity() == null || !symbol.equals(holding.getIdentity().getSymbol())) {
                    continue;
                }
                if (holding.getMarket() != null && holding.getMarket().getCurrentPrice() != null) {
                    return holding.getMarket().getCurrentPrice();
                }
            }
        }
        return null;
    }

    private double resolveChangeMetric(com.am.analysis.adapter.model.AnalysisHolding h, String timeFrame) {
        Double computed = computeChangePercentage(h, timeFrame);
        return computed != null ? computed : 0.0;
    }

    private Double computeChangePercentage(com.am.analysis.adapter.model.AnalysisHolding h, String timeFrame) {
        if (h.getMarket() == null || h.getInvestment() == null) {
            return null;
        }
        if (h.getMarket().getDayChangePercentage() != null) {
            return h.getMarket().getDayChangePercentage();
        }
        Double price = h.getMarket().getCurrentPrice();
        Double reference = h.getMarket().getPreviousClose();
        if (price == null || reference == null) {
            return null;
        }
        return isDailyTimeFrame(timeFrame)
                ? LivePriceOverlayHelper.computeDailyChangePercent(price, reference)
                : LivePriceOverlayHelper.computePeriodChangePercent(price, reference, resolveTimeframe(timeFrame));
    }

    private double resolveChangeAmount(com.am.analysis.adapter.model.AnalysisHolding h, String timeFrame) {
        Double computed = computeChangeAmount(h, timeFrame);
        return computed != null ? computed : 0.0;
    }

    private Double computeChangeAmount(com.am.analysis.adapter.model.AnalysisHolding h, String timeFrame) {
        if (h.getMarket() == null || h.getInvestment() == null) {
            return null;
        }
        if (h.getMarket().getDayChange() != null) {
            return h.getMarket().getDayChange();
        }
        Double price = h.getMarket().getCurrentPrice();
        Double reference = h.getMarket().getPreviousClose();
        double qty = h.getInvestment().getQuantity() != null ? h.getInvestment().getQuantity() : 0.0;
        if (price == null || reference == null || qty <= 0) {
            return null;
        }
        return isDailyTimeFrame(timeFrame)
                ? LivePriceOverlayHelper.computeDailyChangeAmount(qty, price, reference)
                : LivePriceOverlayHelper.computePeriodChangeAmount(qty, price, reference, resolveTimeframe(timeFrame));
    }

    private TopMoversResponse buildTopMoversResponse(List<AnalysisEntity> gainers, List<AnalysisEntity> losers) {
        return TopMoversResponse.builder()
                .gainers(gainers.stream().map(this::mapToMoverItem).toList())
                .losers(losers.stream().map(this::mapToMoverItem).toList())
                .build();
    }

    private TopMoversResponse buildTopMoversResponseFromHoldings(
            List<com.am.analysis.adapter.model.AnalysisHolding> gainers, 
            List<com.am.analysis.adapter.model.AnalysisHolding> losers,
            String timeFrame,
            double totalPortfolioValue) {
        return TopMoversResponse.builder()
                .gainers(gainers.stream().map(h -> mapToMoverItem(h, timeFrame, totalPortfolioValue)).toList())
                .losers(losers.stream().map(h -> mapToMoverItem(h, timeFrame, totalPortfolioValue)).toList())
                .timeFrame(timeFrame)
                .build();
    }

    private TopMoversResponse.MoverItem mapToMoverItem(AnalysisEntity entity) {
        var perf = entity.getPerformance();
        double totalGainLoss = (perf != null && perf.getTotalGainLoss() != null) ? perf.getTotalGainLoss() : 0.0;
        double totalGainLossPct = (perf != null && perf.getTotalGainLossPercentage() != null) ? perf.getTotalGainLossPercentage() : 0.0;
        double totalValue = (perf != null && perf.getTotalValue() != null) ? perf.getTotalValue() : 0.0;

        log.debug("Mapping AnalysisEntity to MoverItem - ID: {}, TotalGainLoss: {}, TotalGainLossPercentage: {}", 
                entity.getSourceId(), totalGainLoss, totalGainLossPct);
        
        return TopMoversResponse.MoverItem.builder()
                .symbol(entity.getSourceId())
                .name(entity.getSourceId())
                .price(BigDecimal.valueOf(totalValue))
                .changePercentage(totalGainLossPct)
                .changeAmount(BigDecimal.valueOf(totalGainLoss))
                .build();
    }

    private String resolveSymbol(com.am.analysis.adapter.model.AnalysisHolding h) {
        if (h == null || h.getIdentity() == null) return null;
        String sym = h.getIdentity().getSymbol();
        if (org.springframework.util.StringUtils.hasText(sym)) {
            return sym;
        }
        if (org.springframework.util.StringUtils.hasText(h.getIdentity().getIsin())) {
            return h.getIdentity().getIsin();
        }
        return h.getIdentity().getName();
    }

    private TopMoversResponse.MoverItem mapToMoverItem(com.am.analysis.adapter.model.AnalysisHolding h, String timeFrame, double totalPortfolioValue) {
        String symbol = resolveSymbol(h) != null ? resolveSymbol(h) : "UNKNOWN";
        String name = (h.getIdentity() != null && h.getIdentity().getName() != null) ? h.getIdentity().getName() : symbol;
        Double currentPrice = (h.getMarket() != null && h.getMarket().getCurrentPrice() != null) ? h.getMarket().getCurrentPrice() : 0.0;
        
        double pct = resolveChangeMetric(h, timeFrame);
        double amt = resolveChangeAmount(h, timeFrame);

        double val = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
        double invested = (h.getInvestment() != null && h.getInvestment().getInvestmentValue() != null) ? h.getInvestment().getInvestmentValue() : 0.0;
        double allocPct = totalPortfolioValue != 0 ? (val / totalPortfolioValue) * 100 : 0.0;
        double pnlPct = invested > 0 ? ((val - invested) / invested) * 100.0 : 0.0;

        return TopMoversResponse.MoverItem.builder()
                .symbol(symbol)
                .name(name)
                .price(BigDecimal.valueOf(currentPrice != null ? currentPrice : 0.0).setScale(2, java.math.RoundingMode.HALF_UP))
                .changePercentage(BigDecimal.valueOf(pct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                .changeAmount(BigDecimal.valueOf(amt).setScale(2, java.math.RoundingMode.HALF_UP))
                .sector(h.getClassification() != null ? h.getClassification().getSector() : "Unknown")
                .assetClass(h.getIdentity() != null ? h.getIdentity().getAssetClass() : "Unknown")
                .marketCapType(h.getClassification() != null ? h.getClassification().getMarketCapType() : "Unknown")
                .quantity(h.getInvestment() != null ? h.getInvestment().getQuantity() : 0.0)
                .currentValue(BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP))
                .investedValue(BigDecimal.valueOf(invested).setScale(2, java.math.RoundingMode.HALF_UP))
                .allocationPercentage(BigDecimal.valueOf(allocPct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                .pnlPercentage(BigDecimal.valueOf(pnlPct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                .build();
    }
    private TopMoversResponse getTopMoversByGroup(List<com.am.analysis.adapter.model.AnalysisHolding> holdings, String timeFrame, double totalPortfolioValue, AnalysisGroupBy groupBy) {
        // Enrich holdings with market data if grouping by classification
        if (groupBy == AnalysisGroupBy.SECTOR || groupBy == AnalysisGroupBy.MARKET_CAP || groupBy == AnalysisGroupBy.ASSET_CLASS) {
            enrichHoldingsWithClassification(holdings);
        }

        boolean usePeriodOverlay = !isDailyTimeFrame(timeFrame);
        
        java.util.function.Function<com.am.analysis.adapter.model.AnalysisHolding, String> classifier = h -> {
            switch (groupBy) {
                case SECTOR: 
                    return (h.getClassification() != null && h.getClassification().getSector() != null) 
                        ? h.getClassification().getSector() : "Unknown";
                case ASSET_CLASS: 
                    return (h.getIdentity() != null && h.getIdentity().getAssetClass() != null) 
                        ? h.getIdentity().getAssetClass() : "Unknown";
                case MARKET_CAP: 
                    return (h.getClassification() != null && h.getClassification().getMarketCapType() != null) 
                        ? h.getClassification().getMarketCapType() : "Unknown";
                default: return "Unknown";
            }
        };

        Map<String, List<com.am.analysis.adapter.model.AnalysisHolding>> groupMap = holdings.stream()
                .collect(java.util.stream.Collectors.groupingBy(classifier));

        List<TopMoversResponse.MoverItem> items = groupMap.entrySet().stream()
            .filter(entry -> !"Unknown".equalsIgnoreCase(entry.getKey()))
            .map(entry -> {
                String groupName = entry.getKey();
                List<com.am.analysis.adapter.model.AnalysisHolding> groupHoldings = entry.getValue();
                
                double groupInceptionValue = groupHoldings.stream()
                    .mapToDouble(h -> {
                        double val = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
                        double pnl = (h.getInvestment() != null && h.getInvestment().getProfitLoss() != null) ? h.getInvestment().getProfitLoss() : 0.0;
                        return val - pnl; // Cost basis
                    })
                    .sum();
                
                double groupCurrentValue = groupHoldings.stream()
                    .mapToDouble(h -> (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0)
                    .sum();
                
                double groupPreviousValue = groupHoldings.stream()
                    .mapToDouble(h -> {
                        double val = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
                        double periodChange = (h.getMarket() != null && h.getMarket().getDayChange() != null) ? h.getMarket().getDayChange() : 0.0;
                        return val - periodChange;
                    })
                    .sum();

                double pct = 0.0;
                double amt = 0.0;

                amt = groupCurrentValue - groupPreviousValue;
                if (groupPreviousValue != 0) {
                    pct = (amt / groupPreviousValue) * 100;
                }

                if (usePeriodOverlay && groupPreviousValue <= 0) {
                    pct = 0.0;
                    amt = 0.0;
                }

                double allocPct = totalPortfolioValue != 0 ? (groupCurrentValue / totalPortfolioValue) * 100 : 0.0;

                return TopMoversResponse.MoverItem.builder()
                        .symbol(groupName)
                        .name(groupName)
                        .price(BigDecimal.valueOf(groupCurrentValue).setScale(2, java.math.RoundingMode.HALF_UP))
                        .changePercentage(BigDecimal.valueOf(pct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .changeAmount(BigDecimal.valueOf(amt).setScale(2, java.math.RoundingMode.HALF_UP))
                        .sector(groupBy == AnalysisGroupBy.SECTOR ? groupName : "Multiple")
                        .currentValue(BigDecimal.valueOf(groupCurrentValue).setScale(2, java.math.RoundingMode.HALF_UP))
                        .investedValue(BigDecimal.valueOf(groupInceptionValue).setScale(2, java.math.RoundingMode.HALF_UP))
                        .allocationPercentage(BigDecimal.valueOf(allocPct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .pnlPercentage(groupInceptionValue != 0 ? ( (groupCurrentValue - groupInceptionValue) / groupInceptionValue * 100 ) : 0.0)
                        .build();
            })
            .collect(java.util.stream.Collectors.toList());

        List<TopMoversResponse.MoverItem> gainers = items.stream()
                .filter(i -> i.getChangePercentage() > 0)
                .sorted((i1, i2) -> Double.compare(i2.getChangePercentage(), i1.getChangePercentage()))
                .limit(10)
                .toList();

        List<TopMoversResponse.MoverItem> losers = items.stream()
                .filter(i -> i.getChangePercentage() < 0)
                .sorted((i1, i2) -> Double.compare(i1.getChangePercentage(), i2.getChangePercentage()))
                .limit(10)
                .toList();

        return TopMoversResponse.builder()
                .gainers(gainers)
                .losers(losers)
                .timeFrame(normalizeTimeFrame(timeFrame))
                .build();
    }

    private void enrichHoldingsWithClassification(List<com.am.analysis.adapter.model.AnalysisHolding> holdings) {
        List<String> symbols = holdings.stream()
                .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                .map(h -> h.getIdentity().getSymbol())
                .distinct()
                .toList();

        if (symbols.isEmpty()) {
            return;
        }

        try {
            Map<String, com.am.portfolio.client.market.model.SecurityMetadata> metadataMap = marketDataClientService.searchSecurities(symbols);

            for (com.am.analysis.adapter.model.AnalysisHolding h : holdings) {
                if (h.getIdentity() != null && h.getIdentity().getSymbol() != null) {
                    com.am.portfolio.client.market.model.SecurityMetadata meta = metadataMap.get(h.getIdentity().getSymbol());
                    if (meta != null) {
                        if (h.getClassification() == null) {
                            h.setClassification(com.am.analysis.adapter.model.components.AssetClassification.builder().build());
                        }
                        
                        // Update Sector if missing or unknown
                        if (h.getClassification().getSector() == null || "Unknown".equalsIgnoreCase(h.getClassification().getSector())) {
                            h.getClassification().setSector(meta.getSector());
                        }
                        
                        // Update Industry if missing or unknown
                        if (h.getClassification().getIndustry() == null || "Unknown".equalsIgnoreCase(h.getClassification().getIndustry())) {
                            h.getClassification().setIndustry(meta.getIndustry());
                        }

                        // Update Market Cap Type if missing or unknown
                        if (h.getClassification().getMarketCapType() == null || "Unknown".equalsIgnoreCase(h.getClassification().getMarketCapType())) {
                            h.getClassification().setMarketCapType(meta.getMarketCapType());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich holdings with market data classification", e);
        }
    }
}
