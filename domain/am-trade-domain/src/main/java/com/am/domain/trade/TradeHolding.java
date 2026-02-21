package com.am.domain.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeHolding {
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal pnl;
    private Double pnlPercentage;
    private BigDecimal investedAmount;
}
