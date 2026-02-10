package com.am.gateway.dto.incoming;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomingPortfolioUpdateDto {
    private String userId;

    @com.fasterxml.jackson.annotation.JsonProperty("equities")
    private List<IncomingEquityHoldingDto> equityHoldings;

    // Summary Fields
    @com.fasterxml.jackson.annotation.JsonProperty("totalValue")
    private Double currentValue;

    @com.fasterxml.jackson.annotation.JsonProperty("totalInvestment")
    private Double investmentValue;

    private Double totalGainLoss;
    private Double totalGainLossPercentage;
    private Double todayGainLoss;
    private Double todayGainLossPercentage;

}
