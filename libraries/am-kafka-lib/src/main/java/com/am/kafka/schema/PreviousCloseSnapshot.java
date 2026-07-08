package com.am.kafka.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviousCloseSnapshot {
    private String id; // The stock/symbol ID e.g. "RELIANCE"
    private String stockName;
    private String snapshotDate;
    private Map<String, Double> previousCloseValues; // keys: Timeframe codes e.g. "1D", "1W", "1M"
}
