package com.am.analysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeatmapResponse {
    private String portfolioId;
    private List<SectorTile> sectors;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SectorTile {
        private String sectorName;
        private double changePercent;   // weighted avg day change of stocks in sector
        private double weightage;       // % of total portfolio value
        private int stockCount;
        private double totalValue;
        private List<StockTile> stocks;
    }

    @Data
    @Builder
    public static class StockTile {
        private String symbol;
        private String name;
        private double value;
        private double changePercent;
        private double weightInSector;
    }
}
