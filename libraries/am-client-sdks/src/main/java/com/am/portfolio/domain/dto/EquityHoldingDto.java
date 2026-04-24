package com.am.portfolio.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquityHoldingDto {
    private String isin;
    private String symbol;
    private Double quantity;
    private Double currentPrice;
    private Double currentValue;
    private Double investmentValue;
    private Double investmentCost;
    private Double profitLoss;
    private Double profitLossPercentage;
    private Double todayProfitLoss;
    private Double todayProfitLossPercentage;
}
