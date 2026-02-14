package com.am.analysis.adapter.model.components;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentStats {
    private Double quantity;
    private Double averagePrice;
    private Double investmentValue;
    private Double currentValue;
    private Double profitLoss;
    private Double profitLossPercentage;
    private Double weight; // Percentage 0-100
    private Double value; // Backward compatibility
}
