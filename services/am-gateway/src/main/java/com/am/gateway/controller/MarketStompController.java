package com.am.gateway.controller;

import com.am.gateway.service.MarketStreamProxyService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP {@code /app/market/*} — proxies upstream symbol connect to am-market (no interest registry).
 */
@Controller
@RequiredArgsConstructor
public class MarketStompController {

    private final MarketStreamProxyService marketStreamProxyService;
    private final StompPrincipalResolver principalResolver;
    private final FlowLogger flowLogger;

    @MessageMapping("/market/subscribe")
    public void subscribe(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = principalResolver.resolve(headerAccessor);
        String sessionId = headerAccessor.getSessionId();

        try (FlowSpan span = flowLogger.start("gateway.stomp.market.subscribe.received",
                "userId", userId,
                "sessionId", sessionId,
                "payload_keys", payload != null ? payload.keySet().toString() : "null")) {

            marketStreamProxyService.connect(userId, payload);
            flowLogger.complete(span);
        }
    }
}
