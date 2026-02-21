package com.am.domain.trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Per-portfolio card shown in the dashboard portfolio list.
 * Supports multi-portfolio users — one PortfolioOverview per portfolio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioOverview {

    // ── Identity ──────────────────────────────────────────────────────────
    private String portfolioId; // Canonical portfolio ID from am-portfolio / am-trade
    private String portfolioName; // User-facing name (e.g. "Growth Fund", "SIP Portfolio")
    private String type; // "Long Term", "Intraday", "SIP", "Trade"

    // ── Portfolio count (for grouped views) ───────────────────────────────
    private int portfolioCount;
    private int holdingCount; // number of unique positions in this portfolio

    // ── Financials ────────────────────────────────────────────────────────
    private BigDecimal totalValue; // Current market value
    private BigDecimal investedValue; // Total cost basis
    private BigDecimal totalReturn; // Absolute gain/loss
    private Double returnPercentage; // % gain/loss on invested
    private BigDecimal dayChange; // Today's change in value
    private Double dayChangePercentage;

    // ── Top holdings (optional: top 3 by value for card preview) ──────────
    private List<String> topSymbols; // e.g. ["AAPL", "RELIANCE", "TCS"]
}
