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
public class MarketStats {
    private Double currentPrice;
    private Double previousClose;
    private Double dayChange;
    private Double dayChangePercentage;
    private LocalDateTime lastUpdatedTime;
}
