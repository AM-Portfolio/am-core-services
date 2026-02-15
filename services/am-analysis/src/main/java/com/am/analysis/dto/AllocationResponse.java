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
    private List<AllocationItem> stocks;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AllocationItem {
        private String name;
        private BigDecimal value;
        private double percentage;
        private List<AllocationHolding> holdings;

        // Performance Metrics
        private Double dayChangePercentage;
        private BigDecimal dayChangeAmount;
        private Double totalChangePercentage;
        private BigDecimal totalChangeAmount;
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

        // Performance Metrics
        private Double dayChangePercentage;
        private BigDecimal dayChangeAmount;
        private Double totalChangePercentage;
        private BigDecimal totalChangeAmount;
    }
}
