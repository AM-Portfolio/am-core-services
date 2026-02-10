package com.am.common.model.events;

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
    private String isin;
    private String symbol;
    private String name;
    private Double quantity;
    private Double currentPrice;
    private Double currentValue;
    private Double investmentValue;
    private Double profitLoss;
    private Double profitLossPercentage;
    private Double todayProfitLoss;
    private Double todayProfitLossPercentage;
}
