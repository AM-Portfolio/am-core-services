package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LivePriceOverlayHelperTest {

    @Test
    void resolveTick_matchesPrefixedSymbol() {
        Map<String, LivePriceTick> ticks = Map.of("ITC", new LivePriceTick(289.28, 289.85));

        LivePriceTick tick = LivePriceOverlayHelper.resolveTick("NSE:ITC", ticks);

        assertNotNull(tick);
        assertEquals(289.28, tick.lastPrice(), 0.01);
    }

    @Test
    void apply_updatesPortfolioTotalsFromTick() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .type(AnalysisEntityType.PORTFOLIO)
                .holdings(List.of(AnalysisHolding.builder()
                        .identity(HoldingIdentity.builder().symbol("NSE:ITC").build())
                        .investment(InvestmentStats.builder()
                                .quantity(10.0)
                                .averagePrice(280.0)
                                .investmentValue(2800.0)
                                .build())
                        .market(MarketStats.builder()
                                .currentPrice(280.0)
                                .previousClose(289.85)
                                .build())
                        .build()))
                .performance(PerformanceSummary.builder()
                        .totalValue(2800.0)
                        .totalInvestment(2800.0)
                        .build())
                .build();

        LivePriceOverlayHelper.apply(entity, Map.of("ITC", new LivePriceTick(291.0, 289.85)));

        assertEquals(2910.0, entity.getPerformance().getTotalValue(), 0.01);
        assertEquals(110.0, entity.getPerformance().getTotalGainLoss(), 0.01);
    }
}
