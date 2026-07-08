package com.am.analysis.adapter.model;

import com.am.kafka.config.KafkaTopics;
import com.am.kafka.config.MarketDataKeys;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Optional;

/**
 * Dashboard widget identity — ties together snapshot storage, Kafka topic, and WebSocket queue.
 */
public enum DashboardWidgetType {

    SUMMARY("summary", KafkaTopics.DASHBOARD_SUMMARY_UPDATE),
    ACTIVITY("activity", KafkaTopics.DASHBOARD_ACTIVITY_UPDATE),
    ALLOCATION("allocation", KafkaTopics.DASHBOARD_ALLOCATION_UPDATE),
    MOVERS("movers", KafkaTopics.DASHBOARD_MOVERS_UPDATE),
    HISTORY("history", KafkaTopics.DASHBOARD_HISTORY_UPDATE);

    /** Lowercase segment used in WebSocket destinations ({@code /queue/dashboard/{code}}). */
    private final String code;

    /** Kafka topic for live widget push (am-analysis → am-gateway). */
    private final String kafkaTopic;

    DashboardWidgetType(String code, String kafkaTopic) {
        this.code = code;
        this.kafkaTopic = kafkaTopic;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    /** STOMP user-private queue, e.g. {@code /user/queue/dashboard/summary}. */
    public String websocketDestination() {
        return "/queue/dashboard/" + code;
    }

    /** MongoDB document id: {@code userId:WIDGET} (enum name, e.g. {@code user123:SUMMARY}). */
    public String documentId(String userId) {
        return userId + ":" + name();
    }

    /** Redis cache key: {@code dashboard:snapshot:{userId}:{WIDGET}}. */
    public String redisKey(String userId) {
        return MarketDataKeys.DASHBOARD_SNAPSHOT_PREFIX + userId + ":" + name();
    }

    @JsonCreator
    public static DashboardWidgetType fromCode(String value) {
        return tryFromCode(value)
                .orElseThrow(() -> new IllegalArgumentException("Unknown dashboard widget: " + value));
    }

    public static Optional<DashboardWidgetType> tryFromCode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(w -> w.code.equalsIgnoreCase(normalized)
                        || w.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
