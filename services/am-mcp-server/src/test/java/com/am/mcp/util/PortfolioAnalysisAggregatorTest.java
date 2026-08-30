package com.am.mcp.util;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioAnalysisAggregatorTest {

    @Test
    void summarizeFlattensNestedHoldings() {
        AnalysisHolding h1 = holding("RELIANCE", 100_000.0, 110_000.0, 10.0);
        AnalysisHolding h2 = holding("TCS", 50_000.0, 45_000.0, -10.0);
        AnalysisEntity entity = AnalysisEntity.builder()
                .sourceId("pf-1")
                .type(AnalysisEntityType.PORTFOLIO)
                .holdings(List.of(h1, h2))
                .build();

        Map<String, Object> summary = PortfolioAnalysisAggregator.summarize(List.of(entity));

        assertThat(summary.get("investmentValue")).isEqualTo(150_000.0);
        assertThat(summary.get("currentValue")).isEqualTo(155_000.0);
        assertThat(summary.get("totalGainLoss")).isEqualTo(5_000.0);
        assertThat(summary.get("totalAssets")).isEqualTo(2);
        assertThat(summary.get("totalHoldings")).isEqualTo(2);
        assertThat(summary.get("gainersCount")).isEqualTo(1);
        assertThat(summary.get("losersCount")).isEqualTo(1);
    }

    @Test
    void listHoldingsUsesEntityPerformanceWhenNoNested() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .sourceId("INFY")
                .type(AnalysisEntityType.HOLDING)
                .performance(PerformanceSummary.builder()
                        .totalInvestment(10_000.0)
                        .totalValue(12_000.0)
                        .totalGainLoss(2_000.0)
                        .totalGainLossPercentage(20.0)
                        .build())
                .build();

        List<Map<String, Object>> rows = PortfolioAnalysisAggregator.listHoldings(List.of(entity));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("symbol")).isEqualTo("INFY");
        assertThat(rows.get(0).get("currentValue")).isEqualTo(12_000.0);
    }

    @Test
    void filterByPortfolioId() {
        AnalysisEntity a = AnalysisEntity.builder().sourceId("pf-a").build();
        AnalysisEntity b = AnalysisEntity.builder().sourceId("pf-b").build();
        assertThat(PortfolioAnalysisAggregator.filterByPortfolioId(List.of(a, b), "pf-b"))
                .containsExactly(b);
        assertThat(PortfolioAnalysisAggregator.filterByPortfolioId(List.of(a, b), null))
                .containsExactly(a, b);
    }

    private static AnalysisHolding holding(String symbol, double invested, double current, double plPct) {
        return AnalysisHolding.builder()
                .identity(HoldingIdentity.builder().symbol(symbol).name(symbol).build())
                .investment(InvestmentStats.builder()
                        .investmentValue(invested)
                        .currentValue(current)
                        .profitLoss(current - invested)
                        .profitLossPercentage(plPct)
                        .quantity(1.0)
                        .build())
                .build();
    }
}
