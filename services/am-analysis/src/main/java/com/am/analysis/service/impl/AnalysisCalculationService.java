package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalysisCalculationService {

    public AllocationResponse calculateAllocation(AnalysisEntity entity) {
        log.debug("Calculating allocation for Entity: {}, TotalValue: {}", entity.getSourceId(), entity.getTotalValue());
        double totalValue = entity.getTotalValue() != null ? entity.getTotalValue() : 0.0;
        List<AllocationResponse.AllocationItem> sectors = new ArrayList<>();
        List<AllocationResponse.AllocationItem> assetClasses = new ArrayList<>();
        List<AllocationResponse.AllocationItem> marketCaps = new ArrayList<>();

        if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
            
            // Group by Sector
            Map<String, List<com.am.analysis.adapter.model.AnalysisHolding>> sectorMap = entity.getHoldings().stream()
                    .filter(h -> h.getSector() != null)
                    .collect(Collectors.groupingBy(com.am.analysis.adapter.model.AnalysisHolding::getSector));
            sectors = buildAllocationItems(sectorMap, totalValue);

            // Group by Asset Class
            Map<String, List<com.am.analysis.adapter.model.AnalysisHolding>> assetMap = entity.getHoldings().stream()
                    .filter(h -> h.getAssetClass() != null)
                    .collect(Collectors.groupingBy(com.am.analysis.adapter.model.AnalysisHolding::getAssetClass));
            assetClasses = buildAllocationItems(assetMap, totalValue);

            // Group by Market Cap
            Map<String, List<com.am.analysis.adapter.model.AnalysisHolding>> marketCapMap = entity.getHoldings().stream()
                    .filter(h -> h.getMarketCapType() != null)
                    .collect(Collectors.groupingBy(com.am.analysis.adapter.model.AnalysisHolding::getMarketCapType));
            marketCaps = buildAllocationItems(marketCapMap, totalValue);
        } else {
             // Fallback
             assetClasses.add(AllocationResponse.AllocationItem.builder()
                    .name("Unknown")
                    .value(BigDecimal.valueOf(totalValue).setScale(2, java.math.RoundingMode.HALF_UP))
                    .percentage(100.0)
                    .holdings(Collections.emptyList())
                    .build());
        }

        return AllocationResponse.builder()
                .portfolioId(entity.getSourceId())
                .sectors(sectors)
                .assetClasses(assetClasses)
                .marketCaps(marketCaps)
                .build();
    }

    private List<AllocationResponse.AllocationItem> buildAllocationItems(
            Map<String, List<com.am.analysis.adapter.model.AnalysisHolding>> groupedHoldings, 
            double totalPortfolioValue) {
        
        List<AllocationResponse.AllocationItem> items = new ArrayList<>();
        
        groupedHoldings.forEach((key, holdings) -> {
            double groupTotal = holdings.stream()
                .mapToDouble(h -> h.getValue() != null ? h.getValue() : 0.0)
                .sum();
                
            double groupPercentage = totalPortfolioValue != 0 ? (groupTotal / totalPortfolioValue) * 100 : 0.0;
            
            List<AllocationResponse.AllocationHolding> itemHoldings = holdings.stream()
                .map(h -> {
                    double hValue = h.getValue() != null ? h.getValue() : 0.0;
                    double hPctInGroup = groupTotal != 0 ? (hValue / groupTotal) * 100 : 0.0;
                    double hPctInPortfolio = totalPortfolioValue != 0 ? (hValue / totalPortfolioValue) * 100 : 0.0;
                    
                    return AllocationResponse.AllocationHolding.builder()
                        .symbol(h.getSymbol())
                        .name(h.getName() != null ? h.getName() : h.getSymbol())
                        .value(BigDecimal.valueOf(hValue).setScale(2, java.math.RoundingMode.HALF_UP))
                        .percentage(BigDecimal.valueOf(hPctInGroup).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .portfolioPercentage(BigDecimal.valueOf(hPctInPortfolio).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                        .build();
                })
                .sorted((h1, h2) -> h2.getValue().compareTo(h1.getValue()))
                .collect(Collectors.toList());

            items.add(AllocationResponse.AllocationItem.builder()
                    .name(key)
                    .value(BigDecimal.valueOf(groupTotal).setScale(2, java.math.RoundingMode.HALF_UP))
                    .percentage(BigDecimal.valueOf(groupPercentage).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue())
                    .holdings(itemHoldings)
                    .build());
        });
        
        items.sort((i1, i2) -> i2.getValue().compareTo(i1.getValue()));
        return items;
    }

    public PerformanceResponse calculatePerformance(AnalysisEntity entity, String timeFrame) {
        log.debug("Calculating performance for Entity: {}, TimeFrame: {}", entity.getSourceId(), timeFrame);
        // In a real scenario, this would potentially fetch historical data or use a time-series DB
        // For now, generating a simulated curve based on the current value and a "trend" if available
        
        List<PerformanceResponse.DataPoint> data = new ArrayList<>();
        double currentValue = entity.getTotalValue() != null ? entity.getTotalValue() : 10000;
        double volatility = 0.02; // 2% daily volatility simulation
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(1); // Default 1M
        
        if ("1Y".equalsIgnoreCase(timeFrame)) {
            startDate = endDate.minusYears(1);
        } else if ("YTD".equalsIgnoreCase(timeFrame)) {
            startDate = LocalDate.of(endDate.getYear(), 1, 1);
        }

        // Generate data points
        LocalDate current = startDate;
        double simValue = currentValue * 0.9; // Start slightly lower to show growth (simulated)
        
        while (!current.isAfter(endDate)) {
            data.add(PerformanceResponse.DataPoint.builder()
                    .date(current)
                    .value(BigDecimal.valueOf(simValue))
                    .build());
            
            // Random walk
            simValue = simValue * (1 + (Math.random() * volatility - (volatility / 2)) + 0.001); 
            current = current.plusDays(1);
        }

        return PerformanceResponse.builder()
                .portfolioId(entity.getSourceId())
                .timeFrame(timeFrame)
                .totalReturnPercentage(entity.getTotalGainLossPercentage() != null ? entity.getTotalGainLossPercentage() : 0.0)
                .totalReturnValue(BigDecimal.valueOf(entity.getTotalGainLoss() != null ? entity.getTotalGainLoss() : 0.0))
                .chartData(data)
                .build();
    }
}
