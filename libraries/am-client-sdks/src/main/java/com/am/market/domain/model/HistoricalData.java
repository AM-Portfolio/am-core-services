package com.am.market.domain.model;

import com.am.market.domain.enums.TimeFrame;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalData {
    private String symbol;
    private TimeFrame interval;
    private List<OHLCVTPoint> dataPoints;
}
