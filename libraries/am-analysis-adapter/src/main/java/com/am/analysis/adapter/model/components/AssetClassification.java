package com.am.analysis.adapter.model.components;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetClassification {
    private String sector;
    private String industry;
    private String marketCapType; // LARGE_CAP, MID_CAP, SMALL_CAP
}
