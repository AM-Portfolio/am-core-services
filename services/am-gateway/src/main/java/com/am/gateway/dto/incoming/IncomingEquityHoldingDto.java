package com.am.gateway.dto.incoming;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomingEquityHoldingDto {
    private String isin;
    private String symbol;
    private Double quantity;
    private Double currentPrice;
    private Double currentValue;
    private Double investmentValue;

    // Maps to 'profitLoss' in source (AssetModel)
    private Double profitLoss;

    // Maps to 'profitLossPercentage' in source
    private Double profitLossPercentage;

    // Maps to 'todayProfitLoss' in source
    private Double todayProfitLoss;

    // Maps to 'todayProfitLossPercentage' in source
    private Double todayProfitLossPercentage;

    // Fields that exist in source but might be filtered out later
    private String sector;
    private String industry;
    private String marketCap;
}
