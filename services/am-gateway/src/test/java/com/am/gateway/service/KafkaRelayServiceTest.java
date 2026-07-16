package com.am.gateway.service;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.gateway.metrics.GatewayBusinessMetrics;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.sanitize.Sanitizer;
import com.am.portfolio.domain.dto.PortfolioUpdateDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaRelayServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AnalysisEventMapper analysisEventMapper;

    @Mock
    private FlowLogger flowLogger;

    @Mock
    private GatewayBusinessMetrics businessMetrics;

    private KafkaRelayService relayService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        relayService = new KafkaRelayService(
                messagingTemplate,
                objectMapper,
                analysisEventMapper,
                flowLogger,
                new Sanitizer(),
                businessMetrics);

        FlowSpan span = mock(FlowSpan.class);
        when(flowLogger.start(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(span);
    }

    @Test
    void handleStockUpdate_relaysEachSymbolToTopic() throws Exception {
        String message = """
                {
                  "eventType": "STOCK_UPDATE",
                  "equityPrices": [
                    {"symbol": "RELIANCE", "lastPrice": 2500.0},
                    {"symbol": "TCS", "lastPrice": 3800.0}
                  ]
                }
                """;

        relayService.handleStockUpdate(message);

        verify(messagingTemplate).convertAndSend(eq("/topic/stock/RELIANCE"), any(String.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/stock/TCS"), any(String.class));
    }

    @Test
    void handlePortfolioStreamUpdate_relaysToUserQueue() {
        String message = """
                {
                  "userId": "user-1",
                  "portfolioId": "pf-1",
                  "totalValue": 100000.0
                }
                """;

        PortfolioUpdateDto dto = PortfolioUpdateDto.builder()
                .userId("user-1")
                .currentValue(100000.0)
                .build();
        when(analysisEventMapper.mapToDto(any())).thenReturn(dto);

        relayService.handlePortfolioStreamUpdate(message);

        verify(messagingTemplate).convertAndSendToUser("user-1", "/queue/portfolio", dto);
    }

    @Test
    void handleDashboardSummaryUpdate_relaysDataNodeToUserQueue() throws Exception {
        String message = """
                {
                  "userId": "user-1",
                  "data": {"totalValue": 50000, "dayChange": 120.5}
                }
                """;

        relayService.handleDashboardSummaryUpdate(message);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("user-1"),
                eq("/queue/dashboard/summary"),
                payloadCaptor.capture());
    }

    @Test
    void handleDashboardUpdate_relaysLegacyTopicPayload() throws Exception {
        String message = """
                {"userId": "user-1", "widget": "summary"}
                """;

        relayService.handleDashboardUpdate(message);

        verify(messagingTemplate).convertAndSend("/topic/dashboard/user-1", message);
    }
}
