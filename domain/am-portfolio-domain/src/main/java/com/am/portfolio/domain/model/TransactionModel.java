package com.am.portfolio.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionModel {
    private LocalDateTime date;
    private Double quantity;
    private Double price;
    private String type; // BUY, SELL, DIVIDEND
    private Double charges;
    private String tradeId;
}
