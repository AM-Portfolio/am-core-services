package com.am.analysis.service.orchestrator;

import com.am.kafka.config.InterestRegistryKeys;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demand-Driven Orchestrator (Phase 3).
 *
 * Responsibilities:
 *   1. Listen for USER_WATCHING events from the Gateway.
 *   2. On first watcher for a portfolio, trigger an immediate calculation.
 *   3. Listen for STOCK_UPDATE events and throttle calculation / dashboard triggers.
 *   4. Temporal Debouncing: max 1 calculation per portfolio per 2 seconds.
 *   5. Dashboard widget pushes for users on {@link InterestRegistryKeys#CHANNEL_DASHBOARD_MAIN}.
 */
@Slf4j
@RequiredArgsConstructor
public class DemandDrivenOrchestrator {

    private final InterestRegistryService interestRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;
    private final TracingHelper tracingHelper;
    private final com.am.analysis.service.DashboardAnalysisService dashboardAnalysisService;

    private static final long DEBOUNCE_WINDOW_MS = 2_000;
    private static final long SUMMARY_DEBOUNCE_MS = 1_000;
    private static final long ACTIVITY_DEBOUNCE_MS = 5_000;
    private static final long MOVERS_DEBOUNCE_MS = 5_000;

    private final Map<String, Long> lastTriggerMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSummaryTrigger = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActivityTrigger = new ConcurrentHashMap<>();
    private final Map<String, Long> lastMoversTrigger = new ConcurrentHashMap<>();

    @KafkaListener(topics = KafkaTopics.USER_WATCHING, groupId = "am-orchestrator-watching-group")
    public void onUserWatching(String message) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.consume.user_watching",
                "payload_bytes", message == null ? 0 : message.length())) {
            try {
                com.am.kafka.schema.UserWatchingEvent event =
                        objectMapper.readValue(message, com.am.kafka.schema.UserWatchingEvent.class);

                if ("SUBSCRIBE".equals(event.getAction())) {
                    if (isDashboardChannel(event.getPortfolioId())) {
                        triggerDashboardSubscribePush(event.getUserId());
                    } else {
                        triggerCalculation(event.getUserId(), event.getPortfolioId(),
                                "USER_SUBSCRIPTION", event.getTraceId());
                    }
                }
                flowLogger.complete(span,
                        "action", event.getAction(),
                        "userId", event.getUserId());
            } catch (Exception e) {
                flowLogger.fail(span, e);
            }
        }
    }

    @KafkaListener(topics = KafkaTopics.STOCK_UPDATE, groupId = "am-orchestrator-stock-group")
    public void onMarketUpdate(String message) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.consume.stock_update",
                "payload_bytes", message == null ? 0 : message.length())) {
            triggerCalculationForActiveWatchers("MARKET_MOVE");
            triggerDashboardUpdatesForActiveWatchers();
            flowLogger.complete(span);
        }
    }

    private void triggerCalculationForActiveWatchers(String source) {
        java.util.Set<String> activeUsers = interestRegistry.getAllActiveUserIds();
        if (activeUsers.isEmpty()) {
            flowLogger.step("analysis.orchestrator.no_active_watchers", "source", source);
            return;
        }
        String traceId = tracingHelper.currentTraceIdOrNew();
        for (String userId : activeUsers) {
            String target = interestRegistry.getWatchedPortfolio(userId).orElse(null);
            if (target != null && !target.isBlank() && !isDashboardChannel(target)) {
                triggerCalculation(userId, target, source, traceId);
            }
        }
    }

    private void triggerDashboardUpdatesForActiveWatchers() {
        java.util.Set<String> activeUsers = interestRegistry.getAllActiveUserIds();
        if (activeUsers.isEmpty()) {
            return;
        }
        try (FlowSpan span = flowLogger.start("analysis.orchestrator.dashboard_fanout",
                "active_users", activeUsers.size())) {
            int dashboardUsers = 0;
            for (String userId : activeUsers) {
                String target = interestRegistry.getWatchedPortfolio(userId).orElse(null);
                if (isDashboardChannel(target)) {
                    dashboardUsers++;
                    triggerDashboardSummaryUpdate(userId, true);
                    triggerDashboardActivityUpdate(userId, true);
                    triggerDashboardMoversUpdate(userId, true);
                }
            }
            flowLogger.complete(span, "dashboard_users", dashboardUsers);
        }
    }

    private void triggerDashboardSubscribePush(String userId) {
        try (FlowSpan span = flowLogger.start("analysis.orchestrator.subscribe_dashboard_push",
                "userId", userId)) {
            long start = System.currentTimeMillis();
            dashboardAnalysisService.publishDashboardSubscribeAll(userId);
            flowLogger.complete(span,
                    "widgets", 5,
                    "duration_ms", System.currentTimeMillis() - start);
        }
    }

    private void triggerDashboardSummaryUpdate(String userId, boolean debounce) {
        long now = System.currentTimeMillis();
        if (debounce) {
            Long lastSummary = lastSummaryTrigger.get(userId);
            if (lastSummary != null && (now - lastSummary) < SUMMARY_DEBOUNCE_MS) {
                flowLogger.step("analysis.orchestrator.dashboard_summary_debounced",
                        "userId", userId, "window_ms", SUMMARY_DEBOUNCE_MS);
                return;
            }
        }
        lastSummaryTrigger.put(userId, now);
        dashboardAnalysisService.publishDashboardSummary(userId);
    }

    private void triggerDashboardActivityUpdate(String userId, boolean debounce) {
        long now = System.currentTimeMillis();
        if (debounce) {
            Long lastActivity = lastActivityTrigger.get(userId);
            if (lastActivity != null && (now - lastActivity) < ACTIVITY_DEBOUNCE_MS) {
                flowLogger.step("analysis.orchestrator.dashboard_activity_debounced",
                        "userId", userId, "window_ms", ACTIVITY_DEBOUNCE_MS);
                return;
            }
        }
        lastActivityTrigger.put(userId, now);
        dashboardAnalysisService.publishDashboardActivity(userId);
    }

    private void triggerDashboardMoversUpdate(String userId, boolean debounce) {
        long now = System.currentTimeMillis();
        if (debounce) {
            Long lastMovers = lastMoversTrigger.get(userId);
            if (lastMovers != null && (now - lastMovers) < MOVERS_DEBOUNCE_MS) {
                flowLogger.step("analysis.orchestrator.dashboard_movers_debounced",
                        "userId", userId, "window_ms", MOVERS_DEBOUNCE_MS);
                return;
            }
        }
        lastMoversTrigger.put(userId, now);
        dashboardAnalysisService.publishDashboardMovers(userId);
    }

    private static boolean isDashboardChannel(String watchTarget) {
        return InterestRegistryKeys.isDashboardChannel(watchTarget);
    }

    private void triggerCalculation(String userId, String portfolioId, String source, String inheritedTraceId) {
        String debounceKey = portfolioId != null ? portfolioId : "global";

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
