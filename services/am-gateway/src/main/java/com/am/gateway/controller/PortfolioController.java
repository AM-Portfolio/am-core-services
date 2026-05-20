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
 * WebSocket controller handling portfolio subscription lifecycle.
 *
 * DESIGN PRINCIPLE: The Gateway is a dumb relay.
 * - It NEVER publishes to TRIGGER_CALCULATION directly.
 * - It only emits USER_WATCHING events (via PortfolioSubscriptionManager → Kafka).
 * - The DemandDrivenOrchestrator (am-analysis) is the SOLE publisher of TRIGGER_CALCULATION.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioSubscriptionManager subscriptionManager;
    private final FlowLogger flowLogger;

    @MessageMapping("/portfolio/subscribe")
    public void subscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = payload.get("userId");
        String portfolioId = payload.get("portfolioId");
        String sessionId = headerAccessor.getSessionId();

        try (FlowSpan span = flowLogger.start("gateway.stomp.subscribe.received",
                "userId", userId,
                "portfolioId", portfolioId != null ? portfolioId : "ALL",
                "sessionId", sessionId,
                "payload_keys", payload.keySet().toString())) {

            if (userId == null) {
                flowLogger.fail(span, null, "reason", "missing_userId");
                return;
            }

            subscriptionManager.onSubscribe(userId, portfolioId, sessionId);
            flowLogger.complete(span);
        }
    }

    @MessageMapping("/portfolio/heartbeat")
    public void heartbeat(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = payload.get("userId");
        String sessionId = headerAccessor.getSessionId();
        if (userId == null) {
            log.warn("Heartbeat missing userId sessionId={}", sessionId);
            return;
        }
        subscriptionManager.onHeartbeat(userId, sessionId);
    }

    @MessageMapping("/portfolio/unsubscribe")
    public void unsubscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = payload.get("userId");
        String sessionId = headerAccessor.getSessionId();
        if (userId == null) {
            log.warn("Unsubscribe missing userId sessionId={}", sessionId);
            return;
        }
        subscriptionManager.onDisconnect(userId, sessionId);
    }

    /**
     * @deprecated The /portfolio/calculate endpoint is removed. Clients should
     *             send to /portfolio/subscribe instead. Routes through the
     *             subscribe pipeline so the Orchestrator triggers a calculation
     *             on first subscription.
     */
    @Deprecated
    @MessageMapping("/portfolio/calculate")
    public void triggerCalculation(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        log.warn("Deprecated /portfolio/calculate received; routing through /subscribe pipeline. Migrate client.");

        String userId = payload.get("userId");
        String portfolioId = payload.get("portfolioId");
        String sessionId = headerAccessor.getSessionId();

        if (userId == null) {
            return;
        }
        subscriptionManager.onSubscribe(userId, portfolioId, sessionId);
    }
}
