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
import static org.junit.jupiter.api.Assertions.assertNull;

class LivePriceOverlayHelperTest {

    @Test
    void looksLikeIsin_detectsStandardIsin() {
        assertEquals(true, LivePriceOverlayHelper.looksLikeIsin("IN0020210228"));
        assertEquals(false, LivePriceOverlayHelper.looksLikeIsin("SGBD29VIII"));
        assertEquals(false, LivePriceOverlayHelper.looksLikeIsin(null));
    }

    @Test
    void storedTradingSymbolFromHoldings_usesMongoTickerNotCompanyName() {
        AnalysisHolding holding = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder()
                        .symbol("GROWWDEFNC")
                        .isin("INF666M01IO8")
                        .companyName("should-not-be-parsed")
                        .build())
                .build();
        AnalysisEntity entity = AnalysisEntity.builder()
                .type(AnalysisEntityType.PORTFOLIO)
                .holdings(List.of(holding))
                .build();

        assertEquals("GROWWDEFNC",
                LivePriceOverlayHelper.storedTradingSymbolFromHoldings("INF666M01IO8", List.of(entity)));
        assertNull(LivePriceOverlayHelper.storedTradingSymbolFromHoldings("INF666M01IO8", List.of()));
    }

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

    @Test
    void apply_rejectsImplausiblePrevClose() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .type(AnalysisEntityType.PORTFOLIO)
                .holdings(List.of(AnalysisHolding.builder()
                        .identity(HoldingIdentity.builder().symbol("IRB").build())
                        .investment(InvestmentStats.builder()
                                .quantity(50.0)
                                .investmentValue(2663.50)
                                .build())
                        .market(MarketStats.builder()
                                .currentPrice(55.93)
                                .previousClose(21.44)
                                .build())
                        .build()))
                .build();

        // Redis prev-close also wrong — daily metrics should be cleared, not 160%+
        LivePriceOverlayHelper.apply(entity, Map.of("IRB", new LivePriceTick(55.93, 21.44)));

        var market = entity.getHoldings().get(0).getMarket();
        assertEquals(null, market.getDayChangePercentage());
        assertEquals(null, market.getPreviousClose());
    }

    @Test
    void inferAveragePrice_fromInvestmentWhenAvgMissing() {
        InvestmentStats inv = InvestmentStats.builder()
                .quantity(50.0)
                .investmentValue(2663.50)
                .build();
        assertEquals(53.27, LivePriceOverlayHelper.inferAveragePrice(inv), 0.01);
    }

    @Test
    void computePeriodChangePercent_allowsLargeWeeklyMove() {
        assertEquals(7.56, LivePriceOverlayHelper.computePeriodChangePercent(
                55.93, 52.0, com.am.kafka.config.Timeframe.ONE_WEEK), 0.01);
    }

    @Test
    void computePeriodChangePercent_rejectsImplausibleReference() {
        assertEquals(null, LivePriceOverlayHelper.computePeriodChangePercent(
                55.93, 21.44, com.am.kafka.config.Timeframe.ONE_WEEK));
    }

    @Test
    void apply_usesPeriodReferenceForOneWeekWindow() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .type(AnalysisEntityType.PORTFOLIO)
                .holdings(List.of(AnalysisHolding.builder()
                        .identity(HoldingIdentity.builder().symbol("IRB").build())
                        .investment(InvestmentStats.builder()
                                .quantity(50.0)
                                .investmentValue(2663.50)
                                .build())
                        .market(MarketStats.builder().currentPrice(55.93).build())
                        .build()))
                .build();

        LivePriceOverlayHelper.apply(entity,
                Map.of("IRB", new LivePriceTick(55.93, 52.0)),
                com.am.kafka.config.Timeframe.ONE_WEEK);

        var market = entity.getHoldings().get(0).getMarket();
        assertEquals(52.0, market.getPreviousClose(), 0.01);
        assertEquals(7.56, market.getDayChangePercentage(), 0.05);
        assertEquals(196.5, market.getDayChange(), 0.5);
    }

    @Test
    void apply_usesPriorCloseForDayChangePercent() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .type(AnalysisEntityType.PORTFOLIO)
                .holdings(List.of(AnalysisHolding.builder()
                        .identity(HoldingIdentity.builder().symbol("ITC").build())
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

        var market = entity.getHoldings().get(0).getMarket();
        assertEquals(11.5, market.getDayChange(), 0.01);
        assertEquals(0.40, market.getDayChangePercentage(), 0.05);
    }

    @Test
    void resolvePrevClose_matchesNseEqAlias() {
        Map<String, Double> redis = Map.of("NSE_EQ:SAIL", 180.05);

        assertEquals(180.05, LivePriceOverlayHelper.resolvePrevClose("SAIL", redis), 0.001);
    }

    @Test
    void expandRedisSymbolKeys_includesNseEqVariant() {
        var keys = LivePriceOverlayHelper.expandRedisSymbolKeys(List.of("SAIL"));

        org.junit.jupiter.api.Assertions.assertTrue(keys.contains("SAIL"));
        org.junit.jupiter.api.Assertions.assertTrue(keys.contains("NSE_EQ:SAIL"));
    }
}
