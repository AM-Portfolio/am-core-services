package com.am.analysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllocationResponse {
    private String portfolioId;
    private List<AllocationItem> sectors;
    private List<AllocationItem> assetClasses;
    private List<AllocationItem> marketCaps;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AllocationItem {
        private String name;
        private BigDecimal value;
        private double percentage;
        private List<AllocationHolding> holdings;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AllocationHolding {
        private String symbol;
        private String name;
        private BigDecimal value;
        private double percentage; // Weight within the category (e.g., % of this Sector)
        private double portfolioPercentage; // Weight within total portfolio
    }
}
