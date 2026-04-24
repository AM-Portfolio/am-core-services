package com.am.analysis.adapter.model.components;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private LocalDateTime date;
    private Double quantity;
    private Double price;
    private String type; // BUY, SELL, DIVIDEND
    private Double charges;
    private String tradeId;
}
