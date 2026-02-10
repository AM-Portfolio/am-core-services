package com.am.analysis.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TopMoversResponse {
    private List<MoverItem> gainers;
    private List<MoverItem> losers;

    @Data
    @Builder
    public static class MoverItem {
        private String symbol;
        private String name;
        private BigDecimal price;
        private double changePercentage;
        private BigDecimal changeAmount;
    }
}
