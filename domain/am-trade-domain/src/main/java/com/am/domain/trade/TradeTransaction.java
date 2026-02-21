package com.am.domain.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeTransaction {
    private String tradeId;
    private String portfolioId;
    private String userId;
    private String symbol;
    private String type; // BUY, SELL
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDateTime date;
    private String status;
    private BigDecimal pnl;
    private Double pnlPercentage;
}
