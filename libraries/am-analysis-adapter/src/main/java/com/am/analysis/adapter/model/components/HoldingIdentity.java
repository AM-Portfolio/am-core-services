package com.am.analysis.adapter.model.components;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldingIdentity {
    private String symbol;
    private String name;
    private String assetClass; // EQUITY, CASH, CRYPTO
    private String isin;
    private String companyName;
    private String exchange;
}
