package com.am.kafka.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event emitted by the Orchestrator to trigger portfolio calculation.
 * Consumers: am-portfolio service.
 * Topic: am-trigger-calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TriggerCalcEvent {
    private String traceId;
    private String spanId;
    private String userId;

    /**
     * The portfolio to calculate. Null means all portfolios for this user.
     */
    private String portfolioId;

    /**
     * Source of the trigger: USER_SUBSCRIPTION, MARKET_MOVE, TRADE_CONFIRMED, SCHEDULER
     */
    private String triggerSource;

    private Instant timestamp;
}
