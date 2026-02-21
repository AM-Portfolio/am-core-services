package com.am.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregate summary across ALL portfolios for the dashboard header.
 * Designed for multi-portfolio users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    // ── Totals ────────────────────────────────────────────────────────────
    private BigDecimal totalValue;
    private BigDecimal totalInvested;
    private BigDecimal totalGainLoss;
    private Double     totalGainLossPercentage;
    private BigDecimal dayChange;
    private Double     dayChangePercentage;

    // ── Portfolio count ───────────────────────────────────────────────────
    private int totalPortfolios;
    private int totalHoldings; // total unique stock positions across all portfolios

    // ── Per-portfolio breakdown (one card per portfolio) ──────────────────
    private List<PortfolioBreakdown> portfolioBreakdown;

    // ── Quick highlights ─────────────────────────────────────────────────
    private PerformerItem bestPerformer;   // symbol with highest profitLossPercent today
    private PerformerItem worstPerformer;  // symbol with lowest  profitLossPercent today

    // ── Currency ──────────────────────────────────────────────────────────
    @Builder.Default
    private String currency = "INR";

    /**
     * false when one or more source services (am-portfolio, am-trade) were unavailable.
     * UI should display a "Partial Data" indicator when false.
     */
    @Builder.Default
    private boolean isComplete = true;

    // ─────────────────────────────────────────────────────────────────────
    // Nested types
    // ─────────────────────────────────────────────────────────────────────

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PortfolioBreakdown {
        private String     portfolioId;
        private String     portfolioName;
        private String     portfolioType;    // "Long Term", "Intraday", "SIP" etc.
        private BigDecimal currentValue;
        private BigDecimal investedValue;
        private BigDecimal gainLoss;
        private Double     gainLossPercent;
        private BigDecimal dayChange;
        private Double     dayChangePercent;
        private int        holdingCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PerformerItem {
        private String symbol;
        private String companyName;
        private Double changePercent;   // today's day change %
        private Double profitLossPercent; // overall P&L %
    }
}
