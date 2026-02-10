package com.am.analysis.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PerformanceResponse {
    private String portfolioId;
    private String timeFrame; // e.g., "1M", "1Y", "YTD"
    private double totalReturnPercentage;
    private BigDecimal totalReturnValue;
    private List<DataPoint> chartData;

    @Data
    @Builder
    public static class DataPoint {
        private LocalDate date;
        private BigDecimal value;
    }
}
