package com.am.analysis.adapter.mapper;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.am.portfolio.domain.model.EquityModel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalysisEventMapperReverseTest {

    private final AnalysisEventMapper mapper = new AnalysisEventMapper();

    @Test
    void mapEntityToPortfolioUpdateEvent_mapsSummaryAndHoldings() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .id("PORTFOLIO_P1")
                .sourceId("P1")
                .ownerId("user1")
                .type(AnalysisEntityType.PORTFOLIO)
                .lastUpdated(LocalDateTime.of(2026, 6, 17, 10, 0))
                .performance(PerformanceSummary.builder()
                        .totalValue(50000.0)
                        .totalInvestment(40000.0)
                        .totalGainLoss(10000.0)
                        .totalGainLossPercentage(25.0)
                        .dayChange(500.0)
                        .dayChangePercentage(1.25)
                        .build())
                .holdings(List.of(AnalysisHolding.builder()
                        .identity(HoldingIdentity.builder()
                                .symbol("TCS")
                                .isin("INE467B01029")
                                .name("TCS")
                                .build())
                        .investment(InvestmentStats.builder()
                                .quantity(10.0)
                                .averagePrice(4000.0)
                                .investmentValue(40000.0)
                                .currentValue(50000.0)
                                .profitLoss(10000.0)
                                .profitLossPercentage(25.0)
                                .build())
                        .market(MarketStats.builder()
                                .currentPrice(5000.0)
                                .previousClose(4950.0)
                                .dayChange(500.0)
                                .dayChangePercentage(1.25)
                                .build())
                        .build()))
                .build();

        PortfolioUpdateEvent event = mapper.mapEntityToPortfolioUpdateEvent(entity);

        assertNotNull(event);
        assertEquals("user1", event.getUserId());
        assertEquals("P1", event.getPortfolioId());
        assertEquals(50000.0, event.getTotalValue());
        assertEquals(40000.0, event.getTotalInvestment());
        assertEquals(10000.0, event.getTotalGainLoss());
        assertEquals(500.0, event.getTodayGainLoss());
        assertEquals(1, event.getEquities().size());

        EquityModel equity = event.getEquities().get(0);
        assertEquals("TCS", equity.getSymbol());
        assertEquals(5000.0, equity.getCurrentPrice());
        assertEquals(10000.0, equity.getProfitLoss());
    }
}
