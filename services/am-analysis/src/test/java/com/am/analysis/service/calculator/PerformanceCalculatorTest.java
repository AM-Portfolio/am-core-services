package com.am.analysis.service.calculator;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.dto.PerformanceResponse;
import com.am.market.domain.model.HistoricalData;
import com.am.market.domain.model.OHLCVTPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PerformanceCalculatorTest {

    private final PerformanceCalculator calculator = new PerformanceCalculator();

    @Test
    void calculate_resolvesIsinAliasForHistoricalLookup() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        OHLCVTPoint yesterdayPoint = OHLCVTPoint.builder()
                .time(yesterday.atStartOfDay())
                .close(50.0)
                .build();
        OHLCVTPoint todayPoint = OHLCVTPoint.builder()
                .time(today.atStartOfDay())
                .close(52.0)
                .build();

        HistoricalData mohealth = HistoricalData.builder()
                .symbol("MOHEALTH")
                .dataPoints(List.of(yesterdayPoint, todayPoint))
                .build();

        AnalysisHolding holding = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder()
                        .symbol("INF247L01BB1")
                        .isin("INF247L01BB1")
                        .build())
                .investment(InvestmentStats.builder()
                        .quantity(12.0)
                        .averagePrice(48.0)
                        .investmentValue(576.0)
                        .currentValue(624.0)
                        .build())
                .market(MarketStats.builder()
                        .currentPrice(52.0)
                        .build())
                .build();

        AnalysisEntity entity = AnalysisEntity.builder()
                .sourceId("ALL")
                .holdings(List.of(holding))
                .lastUpdated(LocalDateTime.now())
                .build();

        Map<String, HistoricalData> marketData = new TreeMap<>();
        marketData.put("MOHEALTH", mohealth);
        marketData.put("INF247L01BB1", mohealth);

        Map<String, String> isinAliases = Map.of("INF247L01BB1", "MOHEALTH");

        PerformanceResponse response = calculator.calculate(
                entity, "1D", marketData, yesterday, today, isinAliases);

        assertNotNull(response.getChartData());
        assertEquals(2, response.getChartData().size());
        BigDecimal lastValue = response.getChartData().get(1).getValue();
        assertEquals(0, BigDecimal.valueOf(624.0).compareTo(lastValue));
    }
}
