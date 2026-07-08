package com.am.gateway.controller;

import com.am.gateway.service.PortfolioSubscriptionManager;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP {@code /app/portfolio/*} — portfolio watch channel + interest registry.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PortfolioStompController {

    private final PortfolioSubscriptionManager subscriptionManager;
    private final StompPrincipalResolver principalResolver;
    private final FlowLogger flowLogger;

    @MessageMapping("/portfolio/subscribe")
    public void subscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = principalResolver.resolve(headerAccessor, payload);
        String portfolioId = payload != null ? payload.get("portfolioId") : null;
        String sessionId = headerAccessor.getSessionId();

        try (FlowSpan span = flowLogger.start("gateway.stomp.subscribe.received",
                "userId", userId,
                "portfolioId", portfolioId != null ? portfolioId : "none",
                "sessionId", sessionId,
                "payload_keys", payload != null ? payload.keySet().toString() : "null")) {

            subscriptionManager.onSubscribe(userId, portfolioId, sessionId);
            flowLogger.complete(span);
        }
    }

    @MessageMapping("/portfolio/heartbeat")
    public void heartbeat(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = principalResolver.resolve(headerAccessor, payload);
        String sessionId = headerAccessor.getSessionId();
        subscriptionManager.onHeartbeat(userId, sessionId);
    }

    @MessageMapping("/portfolio/unsubscribe")
    public void unsubscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = principalResolver.resolve(headerAccessor, payload);
        String sessionId = headerAccessor.getSessionId();
        subscriptionManager.onDisconnect(userId, sessionId);
    }

    /**
     * @deprecated Use {@link #subscribe} — kept for legacy clients.
     */
    @Deprecated
    @MessageMapping("/portfolio/calculate")
    public void triggerCalculation(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        log.warn("Deprecated /portfolio/calculate received; routing through /subscribe pipeline. Migrate client.");
        subscribe(payload, headerAccessor);
    }
}
