package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopMoversAnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisAccessValidator accessValidator;
    private final com.am.market.client.service.MarketDataClientService marketDataClientService;

    public TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId, AnalysisGroupBy groupBy) {
        if (id == null) {
            log.info("Processing Top Movers by Category: Type={}, TimeFrame={}, User={}, GroupBy={}", type, timeFrame, userId, groupBy);
            return getTopMoversByCategory(type, timeFrame, userId, groupBy);
        } else {
            log.info("Processing Top Movers within Entity: ID={}, Type={}, TimeFrame={}, User={}, GroupBy={}", id, type, timeFrame, userId, groupBy);
            return getTopMoversWithinEntity(id, type, timeFrame, userId, groupBy);
        }
    }

    private TopMoversResponse getTopMoversByCategory(AnalysisEntityType type, String timeFrame, String userId, AnalysisGroupBy groupBy) {
        if (type == AnalysisEntityType.PORTFOLIO && userId != null) {
            log.debug("Aggregating portfolio holdings for user: {}", userId);
            List<AnalysisEntity> portfolios = repository.findByOwnerIdAndType(userId, AnalysisEntityType.PORTFOLIO);
            
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
                .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                .collect(java.util.stream.Collectors.toMap(
                    h -> h.getIdentity().getSymbol(),
                    h -> h,
                    (existing, replacement) -> existing // Keep existing
                ));
            
            List<com.am.analysis.adapter.model.AnalysisHolding> holdings = new java.util.ArrayList<>(uniqueHoldings.values());
            
            boolean useDaily = timeFrame == null || "1D".equalsIgnoreCase(timeFrame);

            List<com.am.analysis.adapter.model.AnalysisHolding> gainers = holdings.stream()
                    .sorted((h1, h2) -> Double.compare(getPercentage(h2, useDaily), getPercentage(h1, useDaily))) // Descending
                    .limit(10)
                    .toList();
            
            List<com.am.analysis.adapter.model.AnalysisHolding> losers = holdings.stream()
                    .sorted((h1, h2) -> Double.compare(getPercentage(h1, useDaily), getPercentage(h2, useDaily))) // Ascending
                    .limit(10)
                    .toList();

            return buildTopMoversResponseFromHoldings(gainers, losers, useDaily, totalPortfolioValue);
        }

        // Fallback for non-portfolio types or public types (if any)
        List<AnalysisEntity> gainers = repository.findTop10ByTypeOrderByPerformanceTotalGainLossPercentageDesc(type);
        List<AnalysisEntity> losers = repository.findTop10ByTypeOrderByPerformanceTotalGainLossPercentageAsc(type);
        return buildTopMoversResponse(gainers, losers);
    }

    private TopMoversResponse getTopMoversWithinEntity(String id, AnalysisEntityType type, String timeFrame, String userId, AnalysisGroupBy groupBy) {
        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            AnalysisEntity entity = entityOpt.get();
            accessValidator.verifyAccess(entity, userId);

            if (entity.getHoldings() != null) {
                double totalPortfolioValue = entity.getHoldings().stream()
                    .mapToDouble(h -> (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0)
                    .sum();

                if (groupBy != null && groupBy != AnalysisGroupBy.STOCK) {
                    return getTopMoversByGroup(entity.getHoldings(), timeFrame, totalPortfolioValue, groupBy);
                }

                List<com.am.analysis.adapter.model.AnalysisHolding> holdings = entity.getHoldings();

                boolean useDaily = timeFrame == null || "1D".equalsIgnoreCase(timeFrame);

                List<com.am.analysis.adapter.model.AnalysisHolding> gainers = holdings.stream()
                        .sorted((h1, h2) -> Double.compare(getPercentage(h2, useDaily), getPercentage(h1, useDaily))) // Descending
                        .limit(10)
                        .toList();
                
                List<com.am.analysis.adapter.model.AnalysisHolding> losers = holdings.stream()
                        .sorted((h1, h2) -> Double.compare(getPercentage(h1, useDaily), getPercentage(h2, useDaily))) // Ascending
                        .limit(10)
                        .toList();

                return buildTopMoversResponseFromHoldings(gainers, losers, useDaily, totalPortfolioValue);
            }
        }
        
        return TopMoversResponse.builder().gainers(List.of()).losers(List.of()).build();
    }

    private double getPercentage(com.am.analysis.adapter.model.AnalysisHolding h, boolean useDaily) {
        if (useDaily) {
            return (h.getMarket() != null && h.getMarket().getDayChangePercentage() != null) ? h.getMarket().getDayChangePercentage() : 0.0;
        } else {
            return (h.getInvestment() != null && h.getInvestment().getProfitLossPercentage() != null) ? h.getInvestment().getProfitLossPercentage() : 0.0;
        }
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
            boolean useDaily,
            double totalPortfolioValue) {
        return TopMoversResponse.builder()
                .gainers(gainers.stream().map(h -> mapToMoverItem(h, useDaily, totalPortfolioValue)).toList())
                .losers(losers.stream().map(h -> mapToMoverItem(h, useDaily, totalPortfolioValue)).toList())
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

    private TopMoversResponse.MoverItem mapToMoverItem(com.am.analysis.adapter.model.AnalysisHolding h, boolean useDaily, double totalPortfolioValue) {
        String symbol = h.getIdentity() != null ? h.getIdentity().getSymbol() : "UNKNOWN";
        String name = (h.getIdentity() != null && h.getIdentity().getName() != null) ? h.getIdentity().getName() : symbol;
        Double currentPrice = (h.getMarket() != null) ? h.getMarket().getCurrentPrice() : 0.0;
        
        double pct = 0.0;
        double amt = 0.0;
        
        if (useDaily) {
            if (h.getMarket() != null && h.getMarket().getDayChangePercentage() != null) {
                pct = h.getMarket().getDayChangePercentage();
                amt = (h.getMarket().getDayChange() != null) ? h.getMarket().getDayChange() : 0.0;
            } else if (h.getInvestment() != null) {
                // Fallback to investment stats if market isn't updated
                pct = (h.getInvestment().getProfitLossPercentage() != null) ? h.getInvestment().getProfitLossPercentage() : 0.0;
                amt = (h.getInvestment().getProfitLoss() != null) ? h.getInvestment().getProfitLoss() : 0.0;
            }
        } else {
             pct = (h.getInvestment() != null && h.getInvestment().getProfitLossPercentage() != null) ? h.getInvestment().getProfitLossPercentage() : 0.0;
             amt = (h.getInvestment() != null && h.getInvestment().getProfitLoss() != null) ? h.getInvestment().getProfitLoss() : 0.0;
        }

        double val = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
        double invested = (h.getInvestment() != null && h.getInvestment().getInvestmentValue() != null) ? h.getInvestment().getInvestmentValue() : 0.0;
        double allocPct = totalPortfolioValue != 0 ? (val / totalPortfolioValue) * 100 : 0.0;
        double pnlPct = (h.getInvestment() != null && h.getInvestment().getProfitLossPercentage() != null) ? h.getInvestment().getProfitLossPercentage() : 0.0;

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

        boolean useDaily = timeFrame == null || "1D".equalsIgnoreCase(timeFrame);
        
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
                
                double groupDayPreviousValue = groupHoldings.stream()
                    .mapToDouble(h -> {
                        double val = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
                        double dayChange = (h.getMarket() != null && h.getMarket().getDayChange() != null) ? h.getMarket().getDayChange() : 0.0;
                        return val - dayChange;
                    })
                    .sum();

                double pct = 0.0;
                double amt = 0.0;

                if (useDaily) {
                    amt = groupCurrentValue - groupDayPreviousValue;
                    if (groupDayPreviousValue != 0) {
                        pct = (amt / groupDayPreviousValue) * 100;
                    }
                } else {
                    amt = groupCurrentValue - groupInceptionValue;
                    if (groupInceptionValue != 0) {
                        pct = (amt / groupInceptionValue) * 100;
                    }
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
                .sorted((i1, i2) -> Double.compare(i2.getChangePercentage(), i1.getChangePercentage()))
                .limit(10)
                .toList();

        List<TopMoversResponse.MoverItem> losers = items.stream()
                .sorted((i1, i2) -> Double.compare(i1.getChangePercentage(), i2.getChangePercentage()))
                .limit(10)
                .toList();

        return TopMoversResponse.builder()
                .gainers(gainers)
                .losers(losers)
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
