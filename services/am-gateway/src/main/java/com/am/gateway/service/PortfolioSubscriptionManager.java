package com.am.gateway.service;

import com.am.kafka.config.KafkaTopics;
import com.am.kafka.schema.UserWatchingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Stateless Portfolio Subscription Manager.
 *
 * Responsibilities:
 *   1. Register user subscription in Redis Interest Registry (with TTL).
 *   2. Emit USER_WATCHING event to Kafka so the Orchestrator can decide to trigger calculation.
 *   3. Refresh TTL on heartbeat to prevent ghost-user staleness.
 *   4. Deregister on disconnect.
 *
 * What was REMOVED:
 *   - TaskScheduler: No more periodic per-user schedulers (was a SPOF and resource leak).
 *   - In-memory activeSchedulers map (was stateful, incompatible with horizontal scaling).
 *   - in-memory userPortfolios map (moved to Redis).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSubscriptionManager {

    private final InterestRegistryService interestRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // ────────────────────────────────────────────────────────────────────────
    // Subscription Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Called when a user subscribes to the portfolio WebSocket channel.
     * Registers interest in Redis and emits USER_WATCHING to Kafka.
     *
     * @param userId      Authenticated user ID.
     * @param portfolioId Specific portfolio UUID, or null for all portfolios.
     * @param sessionId   WebSocket session ID.
     */
    public void onSubscribe(String userId, String portfolioId, String sessionId) {
        log.info("[Subscription] User: {} subscribed (Portfolio: {}, Session: {})",
                userId, portfolioId != null ? portfolioId : "ALL", sessionId);

        interestRegistry.register(userId, portfolioId, sessionId);
        emitUserWatchingEvent(userId, portfolioId, sessionId, "SUBSCRIBE");
    }

    /**
     * Called periodically (every 30s) from the WebSocket heartbeat.
     * Keeps the Redis TTL alive so the user isn't treated as disconnected.
     *
     * @param userId    Authenticated user ID.
     * @param sessionId WebSocket session ID.
     */
    public void onHeartbeat(String userId, String sessionId) {
        log.debug("[Heartbeat] User: {}", userId);
        interestRegistry.heartbeat(userId);
        emitUserWatchingEvent(userId, null, sessionId, "HEARTBEAT");
    }

    /**
     * Called when a user explicitly unsubscribes or their WebSocket session disconnects.
     *
     * @param userId    Authenticated user ID.
     * @param sessionId WebSocket session ID.
     */
    public void onDisconnect(String userId, String sessionId) {
        log.info("[Subscription] User: {} disconnected (Session: {})", userId, sessionId);
        interestRegistry.deregister(userId);
        emitUserWatchingEvent(userId, null, sessionId, "UNSUBSCRIBE");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Kafka Event
    // ────────────────────────────────────────────────────────────────────────

    private void emitUserWatchingEvent(String userId, String portfolioId, String sessionId, String action) {
        try {
            UserWatchingEvent event = UserWatchingEvent.builder()
                    .traceId(UUID.randomUUID().toString())
                    .spanId(UUID.randomUUID().toString())
                    .userId(userId)
                    .portfolioId(portfolioId)
                    .action(action)
                    .sessionId(sessionId)
                    .timestamp(Instant.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.USER_WATCHING, userId, payload);
            log.debug("[Subscription] Emitted {} event for User: {}", action, userId);

        } catch (JsonProcessingException e) {
            log.error("[Subscription] Failed to serialize UserWatchingEvent for User: {}", userId, e);
        }
    }
}
