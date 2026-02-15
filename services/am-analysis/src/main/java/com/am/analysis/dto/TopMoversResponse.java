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
        
        // Advanced Fields
        private String sector;
        private String assetClass;
        private String marketCapType;
        private Double quantity;
        private BigDecimal currentValue;
        private BigDecimal investedValue;
        private Double allocationPercentage;
        private Double pnlPercentage;
    }
}
