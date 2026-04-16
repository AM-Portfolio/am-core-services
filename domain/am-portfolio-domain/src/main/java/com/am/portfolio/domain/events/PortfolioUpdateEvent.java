package com.am.portfolio.domain.events;

import com.am.portfolio.domain.model.EquityModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioUpdateEvent {
    private UUID id;
    private String userId;
    private String portfolioId;

    // Core Data
    private List<EquityModel> equities;

    // Summary / Calculation Data
    private Double totalValue;
    private Double totalInvestment;
    private Double totalGainLoss;
    private Double totalGainLossPercentage;
    private Double todayGainLoss;
    private Double todayGainLossPercentage;

    private LocalDateTime timestamp;
}

// event trigger