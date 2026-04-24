package com.am.market.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OHLCVTPoint {
    private LocalDateTime time;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private Integer trades;
}
