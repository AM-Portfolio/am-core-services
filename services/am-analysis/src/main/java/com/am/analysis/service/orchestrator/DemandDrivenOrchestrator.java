package com.am.analysis.service.orchestrator;

import com.am.kafka.config.KafkaTopics;
import com.am.kafka.schema.TriggerCalcEvent;
import com.am.kafka.service.InterestRegistryService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.trace.TracingHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demand-Driven Orchestrator (Phase 3).
 *
 * Responsibilities:
 *   1. Listen for USER_WATCHING events from the Gateway.
 *   2. On first watcher for a portfolio, trigger an immediate calculation.
 *   3. Listen for STOCK_UPDATE events and throttle calculation triggers.
 *   4. Temporal Debouncing: max 1 calculation per portfolio per 2 seconds.
 */
@Slf4j
@RequiredArgsConstructor
public class DemandDrivenOrchestrator {

    private final com.am.kafka.service.InterestRegistryService interestRegistry;
    private final com.am.analysis.service.DashboardAnalysisService dashboardService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;
    private final TracingHelper tracingHelper;

    private static final long DEBOUNCE_WINDOW_MS = 2_000;

    private final Map<String, Long> lastTriggerMap = new ConcurrentHashMap<>();

    @KafkaListener(topics = KafkaTopics.USER_WATCHING, groupId = "am-orchestrator-group")
    public void onUserWatching(String message) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.consume.user_watching",
                "payload_bytes", message == null ? 0 : message.length())) {
            try {
                com.am.kafka.schema.UserWatchingEvent event =
                        objectMapper.readValue(message, com.am.kafka.schema.UserWatchingEvent.class);

                if ("SUBSCRIBE".equals(event.getAction())) {
                    triggerCalculation(event.getUserId(), event.getPortfolioId(),
                            "USER_SUBSCRIPTION", event.getTraceId());
                }
                flowLogger.complete(span,
                        "action", event.getAction(),
                        "userId", event.getUserId());
            } catch (Exception e) {
                flowLogger.fail(span, e);
            }
        }
    }

    /**
     * Listen for real-time stock price updates from Kafka.
     * When a price changes, we:
     *  1. Trigger a background calculation (debounced).
     *  2. Proactively push updated dashboard summaries to all active users via WebSocket.
     */
    @KafkaListener(topics = KafkaTopics.STOCK_UPDATE, groupId = "am-orchestrator-group")
    public void onMarketUpdate(String message) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.consume.stock_update",
                "payload_bytes", message == null ? 0 : message.length())) {
            
            // 1. Start portfolio recalculation (debounced)
            triggerCalculationForActiveWatchers("MARKET_MOVE");
            
            // 2. Proactive Fan-out: Publish dashboard updates to all active users
            // This eliminates the need for UI polling.
            Set<String> activeUsers = interestRegistry.getAllActiveUsers();
            log.debug("[Orchestrator] Market move detected. Pushing dashboard updates to {} active users.", activeUsers.size());
            
            for (String userId : activeUsers) {
                dashboardService.publishDashboardUpdate(userId);
            }
            
            flowLogger.complete(span, "activeUsers", activeUsers.size());
        }
    }

    private void triggerCalculationForActiveWatchers(String source) {
        triggerCalculation(null, null, source, tracingHelper.currentTraceIdOrNew());
    }

    private void triggerCalculation(String userId, String portfolioId, String source, String inheritedTraceId) {
        String debounceKey = portfolioId != null ? portfolioId : "ALL";

        long now = System.currentTimeMillis();
        Long lastTrigger = lastTriggerMap.get(debounceKey);

        if (lastTrigger != null && (now - lastTrigger) < DEBOUNCE_WINDOW_MS) {
            flowLogger.step("analysis.orchestrator.debounced",
                    "portfolioId", debounceKey,
                    "window_ms", DEBOUNCE_WINDOW_MS);
            return;
        }

        if (portfolioId != null && !interestRegistry.hasActiveWatchers(portfolioId)) {
            flowLogger.step("analysis.orchestrator.no_watchers",
                    "portfolioId", portfolioId);
            return;
        }

        lastTriggerMap.put(debounceKey, now);

        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.trigger_calculation",
                "portfolioId", debounceKey,
                "userId", userId,
                "source", source,
                "topic", KafkaTopics.TRIGGER_CALCULATION)) {
            try {
                String traceId = inheritedTraceId != null && !inheritedTraceId.isEmpty()
                        ? inheritedTraceId
                        : tracingHelper.currentTraceIdOrNew();
                String spanId = tracingHelper.currentSpanIdOrNew();

                TriggerCalcEvent event = TriggerCalcEvent.builder()
                        .traceId(traceId)
                        .spanId(spanId)
                        .userId(userId)
                        .portfolioId(portfolioId)
                        .triggerSource(source)
                        .timestamp(Instant.now())
                        .build();

                String payload = objectMapper.writeValueAsString(event);
                String key = portfolioId != null ? portfolioId : (userId != null ? userId : "global");
                kafkaTemplate.send(KafkaTopics.TRIGGER_CALCULATION, key, payload);

                flowLogger.complete(span,
                        "payload_bytes", payload.length(),
                        "trace_id_used", traceId);
            } catch (JsonProcessingException e) {
                flowLogger.fail(span, e);
            }
        }
    }
}
