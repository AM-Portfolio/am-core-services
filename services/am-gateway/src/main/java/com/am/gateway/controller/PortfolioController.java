package com.am.gateway.controller;

import com.am.gateway.service.PortfolioSubscriptionManager;
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
 *   - It NEVER publishes to TRIGGER_CALCULATION directly.
 *   - It only emits USER_WATCHING events (via PortfolioSubscriptionManager → Kafka).
 *   - The DemandDrivenOrchestrator (am-analysis) is the SOLE publisher of TRIGGER_CALCULATION.
 *
 * Flow:
 *   /subscribe   → register in Redis + emit USER_WATCHING → Orchestrator decides to trigger
 *   /heartbeat   → renew Redis TTL (keeps user alive)
 *   /unsubscribe → remove from Redis + emit UNSUBSCRIBE event
 *   /calculate   → REMOVED: No longer needed. Subscribe now drives the trigger pipeline.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final PortfolioSubscriptionManager subscriptionManager;
    // NOTE: GatewayKafkaProducer intentionally NOT injected here.
    // Trigger calculation is owned exclusively by DemandDrivenOrchestrator.

    @MessageMapping("/portfolio/subscribe")
    public void subscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = payload.get("userId");
        String portfolioId = payload.get("portfolioId");
        String sessionId = headerAccessor.getSessionId();

        if (userId == null) {
            log.warn("[PortfolioController] Subscribe missing userId");
            return;
        }

        // Only registers interest + emits USER_WATCHING to Kafka.
        // Orchestrator picks it up and decides whether to trigger calculation.
        subscriptionManager.onSubscribe(userId, portfolioId, sessionId);
    }

    @MessageMapping("/portfolio/heartbeat")
    public void heartbeat(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = payload.get("userId");
        String sessionId = headerAccessor.getSessionId();
        if (userId != null) {
            subscriptionManager.onHeartbeat(userId, sessionId);
        }
    }

    @MessageMapping("/portfolio/unsubscribe")
    public void unsubscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = payload.get("userId");
        String sessionId = headerAccessor.getSessionId();
        if (userId != null) {
            subscriptionManager.onDisconnect(userId, sessionId);
        }
    }

    /**
     * @deprecated The /portfolio/calculate endpoint is removed.
     * Clients should send to /portfolio/subscribe instead.
     * The Orchestrator will immediately trigger a calculation on first subscription.
     *
     * If this breaks any existing client, redirect them to /portfolio/subscribe.
     */
    @MessageMapping("/portfolio/calculate")
    public void triggerCalculation(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        log.warn("[PortfolioController] ⚠️ Deprecated /portfolio/calculate received. " +
                 "Routing through /subscribe pipeline instead. Migrate client to /portfolio/subscribe.");

        String userId = payload.get("userId");
        String portfolioId = payload.get("portfolioId");
        String sessionId = headerAccessor.getSessionId();

        if (userId == null) return;

        // Redirect to the subscribe pipeline — Orchestrator will trigger calc automatically.
        // This eliminates the duplicate publish that was happening before.
        subscriptionManager.onSubscribe(userId, portfolioId, sessionId);
    }
}
