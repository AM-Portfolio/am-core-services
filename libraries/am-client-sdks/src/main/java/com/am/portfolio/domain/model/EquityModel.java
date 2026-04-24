package com.am.portfolio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EquityModel {
    // Basic Identifiers
    private String isin;
    private String symbol;
    private String name;
    private String companyName;

    // Holding Information
    private Double quantity;
    private Double averagePrice;

    // Current Market Data
    private Double currentPrice;
    private Double previousClose;
    private java.time.LocalDateTime lastUpdatedTime;

    // Investment Values
    private Double currentValue;
    private Double investmentValue;

    // Profit/Loss Metrics
    private Double profitLoss;
    private Double profitLossPercentage;

    // Intraday Metrics
    private Double todayProfitLoss;
    private Double todayProfitLossPercentage;
    private Double dayChange;
    private Double dayChangePercentage;

    // Sector/Industry Classification
    private String sector;
    private String industry;
    private String marketCap;
    private String exchange;

    @Builder.Default
    private java.util.List<TransactionModel> transactions = new java.util.ArrayList<>();
}
