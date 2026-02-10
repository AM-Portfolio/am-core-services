package com.am.analysis.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AllocationResponse {
    private String portfolioId;
    private List<AllocationItem> sectors;
    private List<AllocationItem> assetClasses;

    @Data
    @Builder
    public static class AllocationItem {
        private String name;
        private BigDecimal value;
        private double percentage;
    }
}
