package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisCalculationService {

    public AllocationResponse calculateAllocation(AnalysisEntity entity) {
        List<AllocationResponse.AllocationItem> sectors = new ArrayList<>();
        List<AllocationResponse.AllocationItem> assetClasses = new ArrayList<>();

        if (entity.getHoldings() != null && !entity.getHoldings().isEmpty()) {
            // Group by Sector
            Map<String, Double> sectorMap = entity.getHoldings().stream()
                    .filter(h -> h.getSector() != null)
                    .collect(Collectors.groupingBy(
                            h -> h.getSector(),
                            Collectors.summingDouble(h -> h.getValue() != null ? h.getValue() : 0.0)
                    ));

            sectorMap.forEach((k, v) -> sectors.add(AllocationResponse.AllocationItem.builder()
                    .name(k)
                    .value(BigDecimal.valueOf(v))
                    .percentage((v / (entity.getTotalValue() != null ? entity.getTotalValue() : 1)) * 100)
                    .build()));

            // Group by Asset Class
            Map<String, Double> assetMap = entity.getHoldings().stream()
                    .filter(h -> h.getAssetClass() != null)
                    .collect(Collectors.groupingBy(
                            h -> h.getAssetClass(),
                            Collectors.summingDouble(h -> h.getValue() != null ? h.getValue() : 0.0)
                    ));

            assetMap.forEach((k, v) -> assetClasses.add(AllocationResponse.AllocationItem.builder()
                    .name(k)
                    .value(BigDecimal.valueOf(v))
                    .percentage((v / (entity.getTotalValue() != null ? entity.getTotalValue() : 1)) * 100)
                    .build()));
        } else {
            // Fallback if no detailed holdings, maybe use stats or return generic
             assetClasses.add(AllocationResponse.AllocationItem.builder()
                    .name("Unknown")
                    .value(BigDecimal.valueOf(entity.getTotalValue() != null ? entity.getTotalValue() : 0))
                    .percentage(100.0)
                    .build());
        }

        return AllocationResponse.builder()
                .portfolioId(entity.getSourceId()) // Using sourceId as generic ID
                .sectors(sectors)
                .assetClasses(assetClasses)
                .build();
    }

    public PerformanceResponse calculatePerformance(AnalysisEntity entity, String timeFrame) {
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
