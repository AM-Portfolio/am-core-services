package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.service.calculator.PerformanceCalculator;
import com.am.market.client.service.MarketDataClientService;
import com.am.market.domain.enums.TimeFrame;
import com.am.market.domain.model.HistoricalData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisCalculationService {

    private final MarketDataClientService marketDataService;
    private final PerformanceCalculator performanceCalculator;

    public AllocationResponse calculateAllocation(AnalysisEntity entity, AnalysisGroupBy groupBy) {
        double totalValue = (entity.getPerformance() != null && entity.getPerformance().getTotalValue() != null) 
                ? entity.getPerformance().getTotalValue() : 0.0;
        
        log.debug("Calculating allocation for Entity: {}, TotalValue: {}, GroupBy: {}", entity.getSourceId(), totalValue, groupBy);

        List<AllocationResponse.AllocationItem> sectors = null;
        List<AllocationResponse.AllocationItem> assetClasses = null;
        List<AllocationResponse.AllocationItem> marketCaps = null;
        List<AllocationResponse.AllocationItem> stocks = null;

        if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
            if (groupBy == null || groupBy == AnalysisGroupBy.SECTOR) {
                Map<String, List<AnalysisHolding>> sectorMap = entity.getHoldings().stream()
                        .filter(h -> h.getClassification() != null && h.getClassification().getSector() != null)
                        .collect(Collectors.groupingBy(h -> h.getClassification().getSector()));
                sectors = buildAllocationItems(sectorMap, totalValue);
            }

            if (groupBy == null || groupBy == AnalysisGroupBy.ASSET_CLASS) {
                Map<String, List<AnalysisHolding>> assetMap = entity.getHoldings().stream()
                        .filter(h -> h.getIdentity() != null && h.getIdentity().getAssetClass() != null)
                        .collect(Collectors.groupingBy(h -> h.getIdentity().getAssetClass()));
                assetClasses = buildAllocationItems(assetMap, totalValue);
            }

            if (groupBy == null || groupBy == AnalysisGroupBy.MARKET_CAP) {
                Map<String, List<AnalysisHolding>> marketCapMap = entity.getHoldings().stream()
                        .filter(h -> h.getClassification() != null && h.getClassification().getMarketCapType() != null)
                        .collect(Collectors.groupingBy(h -> h.getClassification().getMarketCapType()));
                marketCaps = buildAllocationItems(marketCapMap, totalValue);
            }

            if (groupBy == null || groupBy == AnalysisGroupBy.STOCK) {
                Map<String, List<AnalysisHolding>> stockMap = entity.getHoldings().stream()
                        .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                        .collect(Collectors.groupingBy(h -> h.getIdentity().getSymbol()));
                stocks = buildAllocationItems(stockMap, totalValue);
            }
        }

        return AllocationResponse.builder()
                .portfolioId(entity.getSourceId())
                .sectors(sectors)
                .assetClasses(assetClasses)
                .marketCaps(marketCaps)
                .stocks(stocks)
                .build();
    }
    
    private List<AllocationResponse.AllocationItem> buildAllocationItems(
            Map<String, List<AnalysisHolding>> groupedHoldings, 
            double totalPortfolioValue) {
        
        List<AllocationResponse.AllocationItem> items = new ArrayList<>();
        
        groupedHoldings.forEach((key, holdings) -> {
            double groupTotal = holdings.stream()
                .mapToDouble(h -> (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0)
                .sum();
            
            double groupInceptionValue = holdings.stream()
                .mapToDouble(h -> {
                    double val = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
                    double pnl = (h.getInvestment() != null && h.getInvestment().getProfitLoss() != null) ? h.getInvestment().getProfitLoss() : 0.0;
                    return val - pnl;
                })
                .sum();

            double groupDayPreviousValue = holdings.stream()
                .mapToDouble(h -> {
                    double val = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
                    double dayChange = (h.getMarket() != null && h.getMarket().getDayChange() != null) ? h.getMarket().getDayChange() : 0.0;
                    return val - dayChange;
                })
                .sum();

            double dayAmt = groupTotal - groupDayPreviousValue;
            double dayPct = groupDayPreviousValue != 0 ? (dayAmt / groupDayPreviousValue) * 100 : 0.0;
            double totalAmt = groupTotal - groupInceptionValue;
            double totalPct = groupInceptionValue != 0 ? (totalAmt / groupInceptionValue) * 100 : 0.0;
                
            double groupPercentage = totalPortfolioValue != 0 ? (groupTotal / totalPortfolioValue) * 100 : 0.0;
            
            List<AllocationResponse.AllocationHolding> itemHoldings = holdings.stream()
                .map(h -> {
                    double hValue = (h.getInvestment() != null && h.getInvestment().getValue() != null) ? h.getInvestment().getValue() : 0.0;
                    double hPctInGroup = groupTotal != 0 ? (hValue / groupTotal) * 100 : 0.0;
                    double hPctInPortfolio = totalPortfolioValue != 0 ? (hValue / totalPortfolioValue) * 100 : 0.0;
                    
                    double hDayPct = (h.getMarket() != null && h.getMarket().getDayChangePercentage() != null) ? h.getMarket().getDayChangePercentage() : 0.0;
                    double hDayAmt = (h.getMarket() != null && h.getMarket().getDayChange() != null) ? h.getMarket().getDayChange() : 0.0;
                    double hTotalPct = (h.getInvestment() != null && h.getInvestment().getProfitLossPercentage() != null) ? h.getInvestment().getProfitLossPercentage() : 0.0;
                    double hTotalAmt = (h.getInvestment() != null && h.getInvestment().getProfitLoss() != null) ? h.getInvestment().getProfitLoss() : 0.0;

                    return AllocationResponse.AllocationHolding.builder()
                        .symbol(h.getIdentity() != null ? h.getIdentity().getSymbol() : "UNKNOWN")
                        .name(h.getIdentity() != null && h.getIdentity().getName() != null ? h.getIdentity().getName() : (h.getIdentity() != null ? h.getIdentity().getSymbol() : "UNKNOWN"))
                        .value(BigDecimal.valueOf(hValue).setScale(2, java.math.RoundingMode.HALF_UP))
                        .percentage(BigDecimal.valueOf(hPctInGroup).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .portfolioPercentage(BigDecimal.valueOf(hPctInPortfolio).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .dayChangePercentage(BigDecimal.valueOf(hDayPct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .dayChangeAmount(BigDecimal.valueOf(hDayAmt).setScale(2, java.math.RoundingMode.HALF_UP))
                        .totalChangePercentage(BigDecimal.valueOf(hTotalPct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .totalChangeAmount(BigDecimal.valueOf(hTotalAmt).setScale(2, java.math.RoundingMode.HALF_UP))
                        .build();
                })
                .sorted((h1, h2) -> h2.getValue().compareTo(h1.getValue()))
                .collect(Collectors.toList());

            items.add(AllocationResponse.AllocationItem.builder()
                    .name(key)
                    .value(BigDecimal.valueOf(groupTotal).setScale(2, java.math.RoundingMode.HALF_UP))
                    .percentage(BigDecimal.valueOf(groupPercentage).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                    .holdings(itemHoldings)
                    .dayChangePercentage(BigDecimal.valueOf(dayPct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                    .dayChangeAmount(BigDecimal.valueOf(dayAmt).setScale(2, java.math.RoundingMode.HALF_UP))
                    .totalChangePercentage(BigDecimal.valueOf(totalPct).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                    .totalChangeAmount(BigDecimal.valueOf(totalAmt).setScale(2, java.math.RoundingMode.HALF_UP))
                    .build());
        });
        
        items.sort((i1, i2) -> i2.getValue().compareTo(i1.getValue()));
        return items;
    }

    @Cacheable(value = "performance", key = "#a0.sourceId + '_' + #a1 + '_' + (#a0.lastUpdated != null ? #a0.lastUpdated.toString() : 'null') + '_' + T(java.time.LocalDate).now().toString()")
    public PerformanceResponse calculatePerformance(AnalysisEntity entity, String timeFrame) {
        long startTime = System.currentTimeMillis();
        String entityId = entity.getSourceId();
        log.info("[PerfCalc] Starting calculation for Entity: {}, TimeFrame: {}", entityId, timeFrame);
        
        if (entity.getHoldings() == null || entity.getHoldings().isEmpty()) {
            log.warn("[PerfCalc] No holdings found for Entity: {}. Returning empty response.", entityId);
            return PerformanceResponse.builder()
                    .portfolioId(entityId)
                    .timeFrame(timeFrame)
                    .totalReturnPercentage(0.0)
                    .totalReturnValue(BigDecimal.ZERO)
                    .chartData(Collections.emptyList())
                    .build();
        }

        // 1. Determine Date Range
        LocalDate earliestTxnDate = findEarliestTransactionDate(entity);
        LocalDate lifecycleStartDate = (entity.getLifecycle() != null && entity.getLifecycle().getStartDate() != null) 
                ? entity.getLifecycle().getStartDate().toLocalDate() : null;
        
        LocalDate startDate = earliestTxnDate != null ? earliestTxnDate : lifecycleStartDate;

        LocalDate endDate = (entity.getLifecycle() != null && entity.getLifecycle().getEndDate() != null) 
                ? entity.getLifecycle().getEndDate().toLocalDate() : LocalDate.now();
        LocalDate toDate = endDate; 
        
        LocalDate fromDate;
        if (startDate != null && ("ALL".equalsIgnoreCase(timeFrame) || "TRADE_LIFETIME".equalsIgnoreCase(timeFrame))) {
            fromDate = startDate;
        } else {
            fromDate = calculateFromDate(timeFrame, toDate);
            // Removed clamping to startDate to allow broader historical context even if portfolio started late
        }
        log.info("[PerfCalc] Date Range for {}: {} to {}", entityId, fromDate, toDate);

        // 2. Extract Symbols
        log.debug("[PerfCalc] Inspecting Holdings for Entity {}: Total Holdings = {}", entityId, entity.getHoldings() != null ? entity.getHoldings().size() : "null");
        if (entity.getHoldings() != null && log.isDebugEnabled()) {
            for (int i = 0; i < Math.min(entity.getHoldings().size(), 5); i++) {
                AnalysisHolding h = entity.getHoldings().get(i);
                log.debug("[PerfCalc] Holding [{}]: Identity={}, Symbol={}", i, h.getIdentity(), h.getIdentity() != null ? h.getIdentity().getSymbol() : "null");
            }
        }

        List<String> symbols = entity.getHoldings().stream()
                .filter(h -> h.getIdentity() != null)
                .map(h -> h.getIdentity().getSymbol())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        log.info("[PerfCalc] Extracted {} unique symbols for Entity {}: {}", symbols.size(), entityId, symbols);

        if (symbols.isEmpty()) {
            log.warn("[PerfCalc] No valid symbols found for Entity {}. Skipping Market Data fetch.", entityId);
            return PerformanceResponse.builder()
                    .portfolioId(entityId)
                    .timeFrame(timeFrame)
                    .totalReturnPercentage(0.0)
                    .totalReturnValue(BigDecimal.ZERO)
                    .chartData(Collections.emptyList())
                    .errorMessage("No symbols found in portfolio to analyze.")
                    .build();
        }

            // 3. Batch Fetch Market Data
        Map<String, HistoricalData> marketDataMap = Collections.emptyMap();
        long fetchStart = System.currentTimeMillis();
        try {
            log.info("[PerfCalc] Fetching market data for {} symbols...", symbols.size());
            
            marketDataMap = marketDataService.getHistoricalDataBatch(
                String.join(",", symbols), 
                fromDate.toString(), 
                toDate.toString(), 
                TimeFrame.DAY
            );
            
            log.info("[PerfCalc] Market data fetch completed in {} ms. Received data for {} symbols.", 
                    (System.currentTimeMillis() - fetchStart), marketDataMap.size());
        } catch (Exception e) {
            log.error("[PerfCalc] Failed to fetch batch market data for analysis after {} ms. Error: {}", 
                    (System.currentTimeMillis() - fetchStart), e.getMessage(), e);
            return PerformanceResponse.builder()
                    .portfolioId(entityId)
                    .timeFrame(timeFrame)
                    .errorMessage("Failed to fetch market data: " + e.getMessage())
                    .build();
        }

        // 4. Delegate Calculation
        PerformanceResponse response = performanceCalculator.calculate(entity, timeFrame, marketDataMap, fromDate, toDate);
        log.info("[PerfCalc] Calculation completed in {} ms. Total Return: {}%", (System.currentTimeMillis() - startTime), response.getTotalReturnPercentage());
        return response;
    }

    private LocalDate calculateFromDate(String timeFrame, LocalDate toDate) {
        if (timeFrame == null) return toDate.minusMonths(1);
        
        switch (timeFrame.toUpperCase()) {
            case "1W": return toDate.minusWeeks(1);
            case "1M": return toDate.minusMonths(1);
            case "3M": return toDate.minusMonths(3);
            case "6M": return toDate.minusMonths(6);
            case "1Y": return toDate.minusYears(1);
            case "3Y": return toDate.minusYears(3);
            case "5Y": return toDate.minusYears(5);
            case "YTD": return LocalDate.of(toDate.getYear(), 1, 1);
            case "ALL": return toDate.minusYears(10); // Cap at 10 years or earliest available
            default: return toDate.minusMonths(1);
        }
    }

    private LocalDate findEarliestTransactionDate(AnalysisEntity entity) {
        if (entity.getHoldings() == null) return null;
        
        // 1. Try to find from transactions
        LocalDate earliestTxn = entity.getHoldings().stream()
                .filter(h -> h.getTransactions() != null)
                .flatMap(h -> h.getTransactions().stream())
                .map(com.am.analysis.adapter.model.components.Transaction::getDate)
                .filter(Objects::nonNull)
                .min(java.time.LocalDateTime::compareTo)
                .map(java.time.LocalDateTime::toLocalDate)
                .orElse(null);
                
        if (earliestTxn != null) return earliestTxn;
        
        // 2. Fallback: Find earliest holding start date
        LocalDate earliestHoldingStart = entity.getHoldings().stream()
                .filter(h -> h.getLifecycle() != null && h.getLifecycle().getStartDate() != null)
                .map(h -> h.getLifecycle().getStartDate().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);
                
        if (earliestHoldingStart != null) return earliestHoldingStart;
        
        // 3. Last Fallback: Entity last updated date (or reasonable default)
        return entity.getLastUpdated() != null ? entity.getLastUpdated().toLocalDate() : LocalDate.now();
    }
}

