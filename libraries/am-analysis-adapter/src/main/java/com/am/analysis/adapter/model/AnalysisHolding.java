package com.am.analysis.adapter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHolding {
    // Basic Identifiers
    private String symbol;
    private String name;
    private String assetClass; // EQUITY, CASH, CRYPTO
    private String isin;
    private String companyName;

    // Holding Information
    private Double quantity;
    private Double averagePrice;

    // Current Market Data
    private Double currentPrice;
    private Double previousClose;
    private java.time.LocalDateTime lastUpdatedTime;

    // Investment Values
    private Double investmentValue;
    private Double currentValue;

    // Portfolio Metrics
    private Double value; // Kept for backward compatibility
    private Double weight; // Percentage 0-100

    // Profit/Loss Metrics
    private Double profitLoss;
    private Double profitLossPercentage;

    // Intraday Metrics
    private Double todayProfitLoss;
    private Double todayProfitLossPercentage;
    private Double dayChange;
    private Double dayChangePercentage;

    // Classification (for sector allocation)
    private String sector;
    private String industry;
    private String marketCapType; // LARGE_CAP, MID_CAP, SMALL_CAP
    private String exchange;
}
