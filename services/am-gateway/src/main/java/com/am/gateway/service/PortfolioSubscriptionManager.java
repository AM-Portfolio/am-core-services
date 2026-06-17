package com.am.gateway.service;

import com.am.kafka.config.KafkaTopics;
import com.am.kafka.schema.UserWatchingEvent;
import com.am.kafka.service.InterestRegistryService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.trace.TracingHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Stateless Portfolio Subscription Manager.
 *
 * Responsibilities:
 *   1. Register user subscription in Redis Interest Registry (with TTL).
 *   2. Emit USER_WATCHING event to Kafka so the Orchestrator can decide to trigger calculation.
 *   3. Refresh TTL on heartbeat to prevent ghost-user staleness.
 *   4. Deregister on disconnect.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioSubscriptionManager {

    private final InterestRegistryService interestRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;
    private final TracingHelper tracingHelper;

    public void onSubscribe(String userId, String portfolioId, String sessionId) {
        try (FlowSpan span = flowLogger.start("gateway.stomp.subscribe",
                "userId", userId,
                "portfolioId", portfolioId != null ? portfolioId : "ALL",
                "sessionId", sessionId)) {

            flowLogger.step("gateway.redis.register",
                    "userId", userId, "sessionId", sessionId);
            interestRegistry.register(userId, portfolioId, sessionId);

            emitUserWatchingEvent(userId, portfolioId, sessionId, "SUBSCRIBE");
            flowLogger.complete(span);
        }
    }

    public void onHeartbeat(String userId, String sessionId) {
        try (FlowSpan span = flowLogger.start("gateway.stomp.heartbeat",
                "userId", userId, "sessionId", sessionId)) {
            interestRegistry.heartbeat(userId);
            emitUserWatchingEvent(userId, null, sessionId, "HEARTBEAT");
            flowLogger.complete(span);
        }
    }

    public void onDisconnect(String userId, String sessionId) {
        try (FlowSpan span = flowLogger.start("gateway.stomp.unsubscribe",
                "userId", userId, "sessionId", sessionId)) {
            interestRegistry.deregister(userId);
            emitUserWatchingEvent(userId, null, sessionId, "UNSUBSCRIBE");
            flowLogger.complete(span);
        }
    }

    private void emitUserWatchingEvent(String userId, String portfolioId, String sessionId, String action) {
        try (FlowSpan span = flowLogger.start("gateway.kafka.publish.user_watching",
                "userId", userId, "action", action, "topic", KafkaTopics.USER_WATCHING)) {
            UserWatchingEvent event = UserWatchingEvent.builder()
                    .traceId(tracingHelper.currentTraceIdOrNew())
                    .spanId(tracingHelper.currentSpanIdOrNew())
                    .userId(userId)
                    .portfolioId(portfolioId)
                    .action(action)
                    .sessionId(sessionId)
                    .timestamp(Instant.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.USER_WATCHING, userId, payload);
            flowLogger.complete(span, "payload_bytes", payload.length());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize UserWatchingEvent for User: {}", userId, e);
        }
    }
}
