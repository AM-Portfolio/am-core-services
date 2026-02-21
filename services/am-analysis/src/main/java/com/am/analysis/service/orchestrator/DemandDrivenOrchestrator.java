package com.am.analysis.service.orchestrator;

import com.am.kafka.service.InterestRegistryService;
import com.am.kafka.config.KafkaTopics;
import com.am.kafka.schema.TriggerCalcEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demand-Driven Orchestrator (Phase 3).
 *
 * Responsibilities:
 *   1. Listen for USER_WATCHING events from the Gateway.
 *   2. On first watcher for a portfolio, trigger an immediate calculation.
 *   3. Listen for MARKET_DATA or TRADE events and throttle calculation triggers.
 *   4. Temporal Debouncing: max 1 calculation per portfolio per 2 seconds.
 *
 * Scaling:
 *   - Each Orchestrator instance checks the Redis Interest Registry before sending TRIGGER_CALC.
 *   - If no user is watching the portfolio, no trigger is sent (cost saving).
 *   - Multiple Orchestrator instances: the first to acquire the debounce window wins.
 *     Others see the timestamp and skip (via ConcurrentHashMap; can be moved to Redis for HA).
 *
 * Note: The local debounce map can be promoted to Redis for multi-instance dedup.
 */
@Slf4j
@RequiredArgsConstructor
public class DemandDrivenOrchestrator {

    private final InterestRegistryService interestRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final long DEBOUNCE_WINDOW_MS = 2_000; // 2 seconds

    // portfolioId → lastTriggerTime. Replace with Redis for multi-instance HA.
    private final Map<String, Long> lastTriggerMap = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────────────
    // User Watching Events (from Gateway)
    // ────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = KafkaTopics.USER_WATCHING, groupId = "am-orchestrator-group")
    public void onUserWatching(String message) {
        try {
            com.am.kafka.schema.UserWatchingEvent event =
                    objectMapper.readValue(message, com.am.kafka.schema.UserWatchingEvent.class);

            if ("SUBSCRIBE".equals(event.getAction())) {
                String portfolioId = event.getPortfolioId();
                String userId = event.getUserId();
                log.info("[Orchestrator] New watcher: User={} → Portfolio={}", userId, portfolioId);

                // Trigger an immediate calculation for the newly subscribed portfolio
                triggerCalculation(userId, portfolioId, "USER_SUBSCRIPTION", event.getTraceId());
            }
        } catch (Exception e) {
            log.error("[Orchestrator] Error processing USER_WATCHING event", e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Market Data Events (from am-market-data)
    // ────────────────────────────────────────────────────────────────────────

    @KafkaListener(topics = KafkaTopics.STOCK_UPDATE, groupId = "am-orchestrator-group")
    public void onMarketUpdate(String message) {
        // On market update, trigger calculation for ALL portfolios with active watchers.
        // The debounce window prevents flooding on high-volatility sessions.
        log.debug("[Orchestrator] Market update received, checking active watchers");
        triggerCalculationForActiveWatchers("MARKET_MOVE");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Trigger Logic with Temporal Debouncing
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Trigger a calculation for active watchers of a specific portfolio.
     * Uses debouncing to prevent more than 1 trigger per 2 seconds per portfolio.
     */
    private void triggerCalculationForActiveWatchers(String source) {
        // In a real-world multi-instance setup, query Redis keys directly.
        // Here, a simple market-wide trigger is issued for "ALL" watchers.
        triggerCalculation(null, null, source, UUID.randomUUID().toString());
    }

    /**
     * Core trigger logic with debouncing.
     *
     * @param userId      The requesting user (may be null for market-driven triggers).
     * @param portfolioId The portfolio to calculate (null = all).
     * @param source      Human-readable trigger source for observability.
     * @param traceId     Propagated trace ID.
     */
    private void triggerCalculation(String userId, String portfolioId, String source, String traceId) {
        // Debounce key: portfolio-level (or global for "ALL")
        String debounceKey = portfolioId != null ? portfolioId : "ALL";

        long now = System.currentTimeMillis();
        Long lastTrigger = lastTriggerMap.get(debounceKey);

        if (lastTrigger != null && (now - lastTrigger) < DEBOUNCE_WINDOW_MS) {
            log.debug("[Orchestrator] Debounced trigger for Portfolio={} (within {}ms window)",
                    debounceKey, DEBOUNCE_WINDOW_MS);
            return;
        }

        // Check if any user is actively watching this portfolio
        if (portfolioId != null && !interestRegistry.hasActiveWatchers(portfolioId)) {
            log.debug("[Orchestrator] No active watchers for Portfolio={}. Skipping.", portfolioId);
            return;
        }

        lastTriggerMap.put(debounceKey, now);

        try {
            TriggerCalcEvent event = TriggerCalcEvent.builder()
                    .traceId(traceId != null ? traceId : UUID.randomUUID().toString())
                    .spanId(UUID.randomUUID().toString())
                    .userId(userId)
                    .portfolioId(portfolioId)
                    .triggerSource(source)
                    .timestamp(Instant.now())
                    .build();

            String payload = objectMapper.writeValueAsString(event);
            String key = portfolioId != null ? portfolioId : (userId != null ? userId : "global");
            kafkaTemplate.send(KafkaTopics.TRIGGER_CALCULATION, key, payload);

            log.info("[Orchestrator] ✅ Triggered calculation: Portfolio={}, Source={}, TraceId={}",
                    debounceKey, source, event.getTraceId());

        } catch (JsonProcessingException e) {
            log.error("[Orchestrator] Failed to serialize TriggerCalcEvent", e);
        }
    }
}
