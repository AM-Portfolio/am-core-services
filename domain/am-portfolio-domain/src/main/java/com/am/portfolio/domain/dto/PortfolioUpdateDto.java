package com.am.portfolio.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioUpdateDto {
    private String userId;
    private List<EquityHoldingDto> equities;

    // Summary Fields
    private Double currentValue;
    private Double investmentValue;
    private Double totalGainLoss;
    private Double totalGainLossPercentage;
    private Double todayGainLoss;
    private Double todayGainLossPercentage;
}
