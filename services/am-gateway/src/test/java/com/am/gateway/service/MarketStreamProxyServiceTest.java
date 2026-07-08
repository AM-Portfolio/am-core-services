package com.am.gateway.service;

import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketStreamProxyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FlowLogger flowLogger;

    private MarketStreamProxyService proxyService;

    @BeforeEach
    void setUp() {
        proxyService = new MarketStreamProxyService(restTemplate, flowLogger);
        ReflectionTestUtils.setField(proxyService, "marketDataUrl", "http://market.test:8092");

        FlowSpan span = mock(FlowSpan.class);
        when(flowLogger.start(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(span);
    }

    @Test
    void connect_postsPayloadToMarketStreamConnect() {
        Map<String, Object> body = Map.of(
                "instrumentKeys", java.util.List.of("NSE:RELIANCE"),
                "action", "SUBSCRIBE");

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        proxyService.connect("user-1", body);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForEntity(urlCaptor.capture(), any(HttpEntity.class), eq(String.class));
        assertThat(urlCaptor.getValue()).isEqualTo("http://market.test:8092/v1/market-data/stream/connect");
    }

    @Test
    void connect_skipsEmptyPayload() {
        proxyService.connect("user-1", Map.of());

        verify(restTemplate, org.mockito.Mockito.never())
                .postForEntity(any(String.class), any(HttpEntity.class), eq(String.class));
    }
}
