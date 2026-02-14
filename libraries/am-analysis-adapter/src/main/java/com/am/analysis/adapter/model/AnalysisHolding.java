package com.am.analysis.adapter.model;

import com.am.analysis.adapter.model.components.AssetClassification;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;
import com.am.analysis.adapter.model.components.Lifecycle;
import com.am.analysis.adapter.model.components.MarketStats;
import com.am.analysis.adapter.model.components.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHolding {
    // Basic Identifiers
    private HoldingIdentity identity;

    // Portfolio Specific (Investment Stats) - Likely null for MARKET/INDEX
    private InvestmentStats investment;

    // market Stats (Pricing) - Available for all
    private MarketStats market;

    // Classification (Sector/Industry)
    private AssetClassification classification;

    // Validity
    private Lifecycle lifecycle;
    
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();


}

