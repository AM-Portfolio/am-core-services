package com.am.analysis.service;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.config.PortfolioStreamingProperties;
import com.am.analysis.service.LivePriceTick;
import com.am.kafka.config.AnalysisEntityKeys;
import com.am.kafka.config.KafkaTopics;
import com.am.kafka.service.InterestRegistryService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortfolioStreamingServiceTest {

    @Mock
    private AnalysisRepository analysisRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private FlowLogger flowLogger;
    @Mock
    private InterestRegistryService interestRegistry;

    private AnalysisEventMapper analysisEventMapper;
    private PortfolioStreamingProperties properties;
    private PortfolioStreamingService service;

    @BeforeEach
    void setUp() {
        analysisEventMapper = new AnalysisEventMapper();
        properties = new PortfolioStreamingProperties();
        properties.setEnabled(true);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new PortfolioStreamingService(
                analysisRepository,
                analysisEventMapper,
                kafkaTemplate,
                objectMapper,
                flowLogger,
                interestRegistry,
                properties);

        lenient().when(flowLogger.start(anyString(), any(Object[].class)))
                .thenReturn(mock(FlowSpan.class, RETURNS_DEEP_STUBS));
    }

    @Test
    void publishPortfolioStream_appliesLivePricesAndPublishesKafka() {
        AnalysisEntity entity = buildEntity("user1", "P1", "RELIANCE", 10.0, 2500.0, 2400.0, 25000.0);
        when(analysisRepository.findById("PORTFOLIO_P1")).thenReturn(Optional.of(entity));

        service.publishPortfolioStream("user1", "P1", Map.of("RELIANCE", new LivePriceTick(2600.0, 2400.0)));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.PORTFOLIO_STREAM), eq("user1"), payloadCaptor.capture());

        assertNotNull(payloadCaptor.getValue());
        assertEquals(26000.0, entity.getPerformance().getTotalValue(), 0.01);
        assertEquals(1000.0, entity.getPerformance().getTotalGainLoss(), 0.01);
    }

    @Test
    void publishPortfolioStream_entityNotFound_returnsFalse() {
        when(analysisRepository.findById("PORTFOLIO_P1")).thenReturn(Optional.empty());

        assertFalse(service.publishPortfolioStream("user1", "P1", Map.of()));
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void publishIfUserWatching_skipsWhenNotWatching() {
        AnalysisEntity entity = buildEntity("user1", "P1", "RELIANCE", 10.0, 2500.0, 2400.0, 100000.0);
        when(interestRegistry.getWatchedPortfolio("user1")).thenReturn(Optional.empty());

        service.publishIfUserWatching(entity);

        verify(analysisRepository, never()).findById(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void publishIfUserWatching_publishesWhenPortfolioMatches() {
        AnalysisEntity entity = buildEntity("user1", "P1", "RELIANCE", 10.0, 2500.0, 2400.0, 100000.0);
        when(interestRegistry.getWatchedPortfolio("user1")).thenReturn(Optional.of("P1"));
        when(analysisRepository.findById("PORTFOLIO_P1")).thenReturn(Optional.of(entity));

        service.publishIfUserWatching(entity);

        verify(kafkaTemplate).send(eq(KafkaTopics.PORTFOLIO_STREAM), eq("user1"), any());
    }

    @Test
    void matchesWatchTarget_globalWatchMatchesGlobalEntity() {
        assertTrue(PortfolioStreamingService.matchesWatchTarget("GLOBAL", AnalysisEntityKeys.GLOBAL_SOURCE_ID));
        assertTrue(PortfolioStreamingService.matchesWatchTarget("ALL", AnalysisEntityKeys.GLOBAL_SOURCE_ID));
        assertTrue(PortfolioStreamingService.matchesWatchTarget("P1", "P1"));
        assertFalse(PortfolioStreamingService.matchesWatchTarget("P1", "P2"));
    }

    private AnalysisEntity buildEntity(String userId, String portfolioId, String symbol,
                                         double qty, double price, double prevClose, double investment) {
        return AnalysisEntity.builder()
                .id("PORTFOLIO_" + portfolioId)
                .sourceId(portfolioId)
                .ownerId(userId)
                .type(AnalysisEntityType.PORTFOLIO)
                .lastUpdated(LocalDateTime.now())
                .holdings(List.of(AnalysisHolding.builder()
                        .identity(HoldingIdentity.builder().symbol(symbol).build())
                        .investment(InvestmentStats.builder()
                                .quantity(qty)
                                .averagePrice(investment / qty)
                                .investmentValue(investment)
                                .build())
                        .market(MarketStats.builder()
                                .currentPrice(price)
                                .previousClose(prevClose)
                                .build())
                        .build()))
                .performance(PerformanceSummary.builder()
                        .totalValue(qty * price)
                        .totalInvestment(investment)
                        .build())
                .build();
    }
}
