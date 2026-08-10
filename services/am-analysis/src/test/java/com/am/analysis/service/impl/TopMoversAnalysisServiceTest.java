package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.AssetClassification;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.EntityLoadResult;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import com.am.kafka.config.Timeframe;
import com.am.kafka.service.PreviousCloseRedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopMoversAnalysisServiceTest {

    @Mock
    private AnalysisEntityLoadService entityLoadService;

    @Mock
    private AnalysisAccessValidator accessValidator;

    @Mock
    private com.am.market.client.service.MarketDataClientService marketDataClientService;

    @Mock
    private PreviousCloseRedisService previousCloseRedisService;

    @InjectMocks
    private TopMoversAnalysisService topMoversAnalysisService;

    private AnalysisEntity testPortfolio;

    @BeforeEach
    void setUp() {
        testPortfolio = AnalysisEntity.builder()
                .id("PORTFOLIO_test")
                .sourceId("test")
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user123")
                .holdings(createMockHoldings())
                .build();
    }

    private List<AnalysisHolding> createMockHoldings() {
        // Gainer in Tech
        AnalysisHolding tech1 = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder().symbol("TECH1").name("Tech One").assetClass("EQUITY").build())
                .classification(AssetClassification.builder().sector("Technology").marketCapType("LARGE_CAP").build())
                .investment(InvestmentStats.builder().value(100.0).profitLoss(10.0).build())
                .market(MarketStats.builder().dayChange(5.0).dayChangePercentage(5.0).currentPrice(105.0).build())
                .build();

        // Loser in Tech
        AnalysisHolding tech2 = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder().symbol("TECH2").name("Tech Two").assetClass("EQUITY").build())
                .classification(AssetClassification.builder().sector("Technology").marketCapType("LARGE_CAP").build())
                .investment(InvestmentStats.builder().value(100.0).profitLoss(-10.0).build())
                .market(MarketStats.builder().dayChange(-5.0).dayChangePercentage(-5.0).currentPrice(95.0).build())
                .build();

        // Gainer in Finance
        AnalysisHolding fin1 = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder().symbol("FIN1").name("Fin One").assetClass("EQUITY").build())
                .classification(AssetClassification.builder().sector("Finance").marketCapType("MID_CAP").build())
                .investment(InvestmentStats.builder().value(100.0).profitLoss(20.0).build())
                .market(MarketStats.builder().dayChange(10.0).dayChangePercentage(10.0).currentPrice(110.0).build())
                .build();

        // Null Sector Holding (Should fall into "Unknown" initially, but will be enriched)
        AnalysisHolding unknown = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder().symbol("UNK1").name("Unknown").assetClass("EQUITY").build())
                .classification(AssetClassification.builder().sector(null).marketCapType(null).build())
                .investment(InvestmentStats.builder().value(100.0).profitLoss(5.0).build())
                .market(MarketStats.builder().dayChange(5.0).dayChangePercentage(5.0).currentPrice(105.0).build())
                .build();

        return new java.util.ArrayList<>(Arrays.asList(tech1, tech2, fin1, unknown));
    }

    @Test
    void testGetTopMoversWithinEntity_GroupBySector_WithEnrichment() {
        when(entityLoadService.loadOne(any())).thenReturn(EntityLoadResult.of(List.of(testPortfolio), false));
        
        // Mock Market Data Client
        com.am.portfolio.client.market.model.SecurityMetadata meta = new com.am.portfolio.client.market.model.SecurityMetadata();
        meta.setSector("Healthcare");
        meta.setMarketCapType("SMALL_CAP");
        
        when(marketDataClientService.searchSecurities(anyList())).thenReturn(java.util.Map.of("UNK1", meta));

        TopMoversResponse response = topMoversAnalysisService.getTopMovers(
                "test", AnalysisEntityType.PORTFOLIO, "1D", "user123", AnalysisGroupBy.SECTOR);

        assertNotNull(response);
        
        // "Unknown" holding should have been enriched to "Healthcare"
        Optional<TopMoversResponse.MoverItem> healthItem = response.getGainers().stream()
                .filter(i -> "Healthcare".equals(i.getSymbol()))
                .findFirst();
        
        assertTrue(healthItem.isPresent(), "Should find enriched Healthcare group");
    }


    @Test
    void testGetTopMoversWithinEntity_GroupByMarketCap() {
        when(entityLoadService.loadOne(any())).thenReturn(EntityLoadResult.of(List.of(testPortfolio), false));

        TopMoversResponse response = topMoversAnalysisService.getTopMovers(
                "test", AnalysisEntityType.PORTFOLIO, "1D", "user123", AnalysisGroupBy.MARKET_CAP);

        assertNotNull(response);
        
        // Group Map:
        // MID_CAP (Finance): 11.11%
        // LARGE_CAP (Tech1, Tech2): 0%
        
        Optional<TopMoversResponse.MoverItem> midCapItem = response.getGainers().stream()
                .filter(i -> "MID_CAP".equals(i.getSymbol()))
                .findFirst();
        
        assertTrue(midCapItem.isPresent());
        assertEquals(11.11, midCapItem.get().getChangePercentage(), 0.01);
    }

    @Test
    void testGetTopMoversWithinEntity_WithNullClassification_NoNPE() {
        // Add a holding with null classification object entirely
        testPortfolio.getHoldings().add(AnalysisHolding.builder()
                .identity(HoldingIdentity.builder().symbol("NULL1").build())
                .classification(null)
                .investment(InvestmentStats.builder().value(100.0).build())
                .market(MarketStats.builder().dayChange(0.0).dayChangePercentage(0.0).build())
                .build());

        when(entityLoadService.loadOne(any())).thenReturn(EntityLoadResult.of(List.of(testPortfolio), false));

        // Should not throw NPE
        assertDoesNotThrow(() -> topMoversAnalysisService.getTopMovers(
                "test", AnalysisEntityType.PORTFOLIO, "1D", "user123", AnalysisGroupBy.SECTOR));
    }

    @Test
    void testGetTopMoversByCategory_usesRedisPrevCloseAndMongoPrice() {
        AnalysisHolding sail = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder().symbol("SAIL").name("SAIL").assetClass("EQUITY").build())
                .investment(InvestmentStats.builder()
                        .quantity(10.0)
                        .averagePrice(170.0)
                        .investmentValue(1700.0)
                        .value(1820.0)
                        .build())
                .market(MarketStats.builder().currentPrice(182.0).build())
                .build();

        AnalysisEntity portfolio = AnalysisEntity.builder()
                .id("PORTFOLIO_p1")
                .sourceId("p1")
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user123")
                .holdings(List.of(sail))
                .performance(com.am.analysis.adapter.model.components.PerformanceSummary.builder()
                        .totalValue(1820.0)
                        .build())
                .build();

        when(entityLoadService.loadPortfoliosForUser(eq("user123"), any()))
                .thenReturn(EntityLoadResult.of(List.of(portfolio), false));
        when(previousCloseRedisService.readWindowForSymbols(anyCollection(), eq(Timeframe.ONE_DAY)))
                .thenReturn(java.util.Map.of("NSE_EQ:SAIL", 180.05));

        TopMoversResponse response = topMoversAnalysisService.getTopMovers(
                null, AnalysisEntityType.PORTFOLIO, "1D", "user123", AnalysisGroupBy.STOCK);

        assertNotNull(response);
        assertEquals(1, response.getGainers().size());
        assertEquals("SAIL", response.getGainers().get(0).getSymbol());
        assertTrue(response.getGainers().get(0).getChangePercentage() > 0);
        assertTrue(response.getLosers().isEmpty());
    }

    @Test
    void isinStoredAsSymbol_resolvesToTickerForDisplay() {
        AnalysisHolding isinHolding = AnalysisHolding.builder()
                .identity(HoldingIdentity.builder()
                        .symbol("INE002A01018")
                        .isin("INE002A01018")
                        .companyName("Reliance Industries")
                        .assetClass("EQUITY")
                        .build())
                .investment(InvestmentStats.builder().value(1000.0).profitLoss(50.0).build())
                .market(MarketStats.builder().dayChange(10.0).dayChangePercentage(1.0).currentPrice(100.0).build())
                .build();

        AnalysisEntity portfolio = AnalysisEntity.builder()
                .id("PORTFOLIO_p2")
                .sourceId("p2")
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user123")
                .holdings(List.of(isinHolding))
                .performance(com.am.analysis.adapter.model.components.PerformanceSummary.builder()
                        .totalValue(1000.0)
                        .build())
                .build();

        when(entityLoadService.loadPortfoliosForUser(eq("user123"), any()))
                .thenReturn(EntityLoadResult.of(List.of(portfolio), false));
        when(marketDataClientService.resolveIsinsToTickers(anyList()))
                .thenReturn(java.util.Map.of("INE002A01018", "RELIANCE"));
        when(previousCloseRedisService.readWindowForSymbols(anyCollection(), eq(Timeframe.ONE_DAY)))
                .thenReturn(java.util.Map.of());
        when(marketDataClientService.getQuotes(anyString(), anyString(), any()))
                .thenReturn(java.util.Map.of());

        TopMoversResponse response = topMoversAnalysisService.getTopMovers(
                null, AnalysisEntityType.PORTFOLIO, "1D", "user123", AnalysisGroupBy.STOCK);

        assertEquals(1, response.getGainers().size());
        assertEquals("RELIANCE", response.getGainers().get(0).getSymbol());
        assertEquals("Reliance Industries", response.getGainers().get(0).getName());
    }
}
