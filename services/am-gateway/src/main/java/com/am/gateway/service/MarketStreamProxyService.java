package com.am.gateway.service;

import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Server-side proxy: gateway STOMP {@code /app/market/subscribe} → am-market REST connect.
 * Keeps Flutter clients off am-market directly.
 */
@Service
@Slf4j
public class MarketStreamProxyService {

    private static final String CONNECT_PATH = "/v1/market-data/stream/connect";

    private final RestTemplate marketStreamRestTemplate;
    private final FlowLogger flowLogger;

    @Value("${am.services.market-data.url:http://localhost:8092}")
    private String marketDataUrl;

    public MarketStreamProxyService(
            @Qualifier("marketStreamRestTemplate") RestTemplate marketStreamRestTemplate,
            FlowLogger flowLogger) {
        this.marketStreamRestTemplate = marketStreamRestTemplate;
        this.flowLogger = flowLogger;
    }

    public void connect(String userId, Map<String, Object> body) {
        try (FlowSpan span = flowLogger.start("gateway.market.connect.proxy",
                "userId", userId,
                "market_url", marketDataUrl)) {
            if (body == null || body.isEmpty()) {
                flowLogger.fail(span, null, "reason", "empty_payload");
                log.warn("[MarketProxy] Empty connect payload for user {}", userId);
                return;
            }

            String url = marketDataUrl.replaceAll("/$", "") + CONNECT_PATH;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            flowLogger.step("gateway.market.connect.post", "url", url);
            ResponseEntity<String> response =
                    marketStreamRestTemplate.postForEntity(url, request, String.class);

            flowLogger.complete(span,
                    "status", response.getStatusCode().value(),
                    "response_bytes", response.getBody() != null ? response.getBody().length() : 0);
            log.info("[MarketProxy] Connect proxied for user {} → status {}", userId, response.getStatusCode());
        } catch (Exception e) {
            log.error("[MarketProxy] Failed to proxy market connect for user {}", userId, e);
        }
    }
}
