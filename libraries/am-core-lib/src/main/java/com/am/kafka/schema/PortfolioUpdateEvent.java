package com.am.kafka.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event emitted after portfolio calculation is complete.
 * Consumed by Gateway to relay live updates to the Dashboard UI.
 * Topic: am-portfolio-stream
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioUpdateEvent {
    private String traceId;
    private String spanId;
    private String userId;
    private String portfolioId;

    // Aggregated summary data
    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal totalGainLoss;
    private Double totalGainLossPercentage;
    private BigDecimal dayChange;
    private Double dayChangePercentage;
    private Integer totalPortfolios;

    /**
     * true = all data present, false = partial (a source service was degraded)
     */
    private boolean isComplete;
    private Instant timestamp;
}
