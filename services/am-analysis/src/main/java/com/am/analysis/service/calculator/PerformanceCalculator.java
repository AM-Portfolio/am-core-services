package com.am.analysis.service.calculator;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.dto.PerformanceResponse;
import com.am.market.domain.model.HistoricalData;
import com.am.market.domain.model.OHLCVTPoint;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class PerformanceCalculator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PerformanceCalculator.class);

    public PerformanceResponse calculate(AnalysisEntity entity, String timeFrame, Map<String, HistoricalData> marketDataMap, LocalDate fromDate, LocalDate toDate) {
        
        // 1. Process Market Data into efficient lookup map
        log.debug("[PerfCalc] Building price history map from {} market data entries.", marketDataMap != null ? marketDataMap.size() : 0);
        Map<String, NavigableMap<LocalDate, Double>> priceHistoryMap = buildPriceHistoryMap(marketDataMap);

        // 2. Iterate and Calculate Daily Values
        List<PerformanceResponse.DataPoint> chartData = new ArrayList<>();
        LocalDate currentDate = fromDate;
        
        BigDecimal firstValue = null;
        BigDecimal lastValue = null;
        double firstInvestedValue = 0.0;
        double lastInvestedValue = 0.0;
        
        int missingDataCount = 0;

        while (!currentDate.isAfter(toDate)) {
            double dailyTotalValue = 0.0;
            double dailyInvestedValue = 0.0;
            boolean hasData = false;

            for (AnalysisHolding holding : entity.getHoldings()) {
                if (holding.getIdentity() == null) continue;

                String sym = holding.getIdentity().getSymbol();
                
                // User Suggestion: Use transactions for quantity for better exactness
                Double qtyAtDate = calculateQuantityAtDate(holding, currentDate, entity);
                if (qtyAtDate <= 0) continue;

                Double avgPrice = (holding.getInvestment() != null) ? holding.getInvestment().getAveragePrice() : null;
                
                NavigableMap<LocalDate, Double> history = priceHistoryMap.get(sym);
                if (history != null) {
                    Map.Entry<LocalDate, Double> priceEntry = history.floorEntry(currentDate);
                    
                    if (priceEntry != null) {
                        double price = priceEntry.getValue();
                        dailyTotalValue += (price * qtyAtDate);
                        
                        double cost = (avgPrice != null && avgPrice > 0) ? avgPrice : price;
                        dailyInvestedValue += (cost * qtyAtDate);
                        
                        hasData = true; 
                    } else {
                        // Log only once per symbol/day combo if needed, or aggregate
                    }
                } else {
                     if (currentDate.equals(toDate)) { // Log missing data only for the last day to avoid spam
                         log.trace("[PerfCalc] No price history found for symbol: {}", sym);
                     }
                }
            }

            if (hasData) {
                BigDecimal val = BigDecimal.valueOf(dailyTotalValue);
                chartData.add(PerformanceResponse.DataPoint.builder()
                        .date(currentDate)
                        .value(val)
                        .build());
                
                if (firstValue == null) {
                    LocalDate portfolioStart = getPortfolioStartDate(entity);
                    // If calculation starts at portfolio inception (or earlier), use Cost Basis as baseline
                    if (portfolioStart != null && !currentDate.isAfter(portfolioStart)) {
                        firstValue = BigDecimal.valueOf(dailyInvestedValue);
                    } else {
                        firstValue = val;
                    }
                    firstInvestedValue = dailyInvestedValue;
                }
                lastValue = val;
                lastInvestedValue = dailyInvestedValue;
            } else {
                missingDataCount++;
            }
            
            currentDate = currentDate.plusDays(1);
        }

        if (missingDataCount > 0) {
             log.debug("[PerfCalc] Missing data for {} days out of total range.", missingDataCount);
        }

        // 3. Compute Aggregates
        return buildResponse(entity, timeFrame, chartData, firstValue, lastValue, firstInvestedValue, lastInvestedValue);
    }

    private LocalDate getPortfolioStartDate(AnalysisEntity entity) {
        if (entity.getHoldings() == null) return null;
        
        // 1. Transaction Date
        LocalDate earliestTxn = entity.getHoldings().stream()
                .filter(h -> h.getTransactions() != null)
                .flatMap(h -> h.getTransactions().stream())
                .map(com.am.analysis.adapter.model.components.Transaction::getDate)
                .filter(Objects::nonNull)
                .min(java.time.LocalDateTime::compareTo)
                .map(java.time.LocalDateTime::toLocalDate)
                .orElse(null);
                
        if (earliestTxn != null) return earliestTxn;
        
        // 2. Lifecycle Start
        return entity.getHoldings().stream()
                .filter(h -> h.getLifecycle() != null && h.getLifecycle().getStartDate() != null)
                .map(h -> h.getLifecycle().getStartDate().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private Double calculateQuantityAtDate(AnalysisHolding holding, LocalDate date, AnalysisEntity entity) {
        if (holding.getTransactions() == null || holding.getTransactions().isEmpty()) {
            // Fallback to current quantity if no transactions
            // Use current quantity if the date is within the holding's lifecycle 
            // OR if it's before the entity's last updated date (assuming it was held until then)
            if (isWithinLifecycle(holding, date, entity)) {
                return (holding.getInvestment() != null && holding.getInvestment().getQuantity() != null) 
                        ? holding.getInvestment().getQuantity() : 0.0;
            }
            return 0.0;
        }

        return holding.getTransactions().stream()
                .filter(t -> t.getDate() != null && !t.getDate().toLocalDate().isAfter(date))
                .mapToDouble(t -> {
                    double q = t.getQuantity() != null ? t.getQuantity() : 0.0;
                    if ("SELL".equalsIgnoreCase(t.getType())) return -q;
                    return q;
                })
                .sum();
    }

    private boolean isWithinLifecycle(AnalysisHolding holding, LocalDate date, AnalysisEntity entity) {
        var lifecycle = holding.getLifecycle();
        
        // If no explicit lifecycle, use entity's lastUpdated as the 'known present'
        LocalDate knownPresent = entity.getLastUpdated() != null ? entity.getLastUpdated().toLocalDate() : LocalDate.now();
        
        LocalDate holdStart = (lifecycle != null && lifecycle.getStartDate() != null) 
                ? lifecycle.getStartDate().toLocalDate() : LocalDate.MIN; // Assume always held if no start date
        
        LocalDate holdEnd = (lifecycle != null && lifecycle.getEndDate() != null) 
                ? lifecycle.getEndDate().toLocalDate() : knownPresent;
                
        return !date.isBefore(holdStart) && !date.isAfter(holdEnd);
    }

    private Map<String, NavigableMap<LocalDate, Double>> buildPriceHistoryMap(Map<String, HistoricalData> marketDataMap) {
        Map<String, NavigableMap<LocalDate, Double>> priceHistoryMap = new HashMap<>();
        if (marketDataMap == null) return priceHistoryMap;

        for (Map.Entry<String, HistoricalData> entry : marketDataMap.entrySet()) {
            String symbol = entry.getKey();
            HistoricalData data = entry.getValue();
            
            NavigableMap<LocalDate, Double> closeMap = new TreeMap<>();
            if (data != null && data.getDataPoints() != null) {
                for (OHLCVTPoint p : data.getDataPoints()) {
                    if (p.getTime() != null && p.getClose() != null) {
                        closeMap.put(p.getTime().toLocalDate(), p.getClose());
                    }
                }
            }
            priceHistoryMap.put(symbol, closeMap);
        }
        return priceHistoryMap;
    }

    private PerformanceResponse buildResponse(AnalysisEntity entity, String timeFrame, List<PerformanceResponse.DataPoint> chartData, 
                                            BigDecimal firstValue, BigDecimal lastValue, double firstInvested, double lastInvested) {
        
        double totalReturnPct = 0.0;
        double totalReturnVal = 0.0;

        if (firstValue != null && lastValue != null) {
            double marketValueChange = lastValue.doubleValue() - firstValue.doubleValue();
            double investedChange = lastInvested - firstInvested;
            
            totalReturnVal = marketValueChange - investedChange;
            double denominator = firstValue.doubleValue() > 0 ? firstValue.doubleValue() : firstInvested;
            
            if (denominator > 0) {
                totalReturnPct = (totalReturnVal / denominator) * 100.0;
            }
        }

        return PerformanceResponse.builder()
                .portfolioId(entity.getSourceId())
                .timeFrame(timeFrame)
                .totalReturnPercentage(totalReturnPct)
                .totalReturnValue(BigDecimal.valueOf(totalReturnVal))
                .chartData(chartData)
                .build();
    }
}
