package com.am.kafka.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event emitted by the Gateway when a user subscribes to live portfolio updates.
 * Used by the Orchestrator to decide whether to trigger calculations.
 * Topic: am-user-watching
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserWatchingEvent {
    /**
     * Unique correlation ID for distributed tracing.
     */
    private String traceId;
    private String spanId;

    private String userId;

    /**
     * The specific portfolio being watched. Null = watching all portfolios.
     */
    private String portfolioId;

    /**
     * SUBSCRIBE or HEARTBEAT or UNSUBSCRIBE
     */
    private String action;

    private Instant timestamp;

    /**
     * WebSocket session ID for targeted delivery.
     */
    private String sessionId;
}
