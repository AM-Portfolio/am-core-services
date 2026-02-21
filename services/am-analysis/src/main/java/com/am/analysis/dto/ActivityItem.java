package com.am.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Rich activity item representing a single portfolio holding or event.
 *
 * type = HOLDING         → live position with full P&L metrics
 * type = PORTFOLIO_UPDATE → portfolio-level recalculation event
 * type = ALERT           → system notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityItem {

    private String id;

    /** Typed discriminator — replaces raw String. Use ActivityType enum. */
    private ActivityType type;

    // ── Portfolio Identity ─────────────────────────────────────────────────
    /**
     * The source portfolio ID this holding/event belongs to.
     * Critical when viewing activity across ALL portfolios — lets the UI
     * group or link back to the correct portfolio.
     */
    private String portfolioId;
    private String portfolioName;   // Human-readable name

    // ── Holding Identity ──────────────────────────────────────────────────
    private String symbol;          // e.g. "AAPL"
    private String companyName;     // e.g. "Apple Inc."
    private String exchange;        // e.g. "NSE", "BSE", "NASDAQ"
    private String sector;          // e.g. "Technology" — used for filter grouping

    // ── Position Details ──────────────────────────────────────────────────
    private Double quantity;        // Number of shares/units held
    private Double avgBuyingPrice;  // Average cost basis per unit
    private Double currentPrice;    // Live market price per unit
    private Double investmentValue; // Total cost (qty × avgBuyingPrice)
    private Double currentValue;    // Current market value (qty × currentPrice)

    // ── P&L ───────────────────────────────────────────────────────────────
    private Double profitLoss;          // Absolute P&L (currentValue - investmentValue)
    private Double profitLossPercent;   // % gain/loss on investment
    private Double dayChange;           // Intraday change in value
    private Double dayChangePercent;    // Intraday change %

    /**
     * WIN  = profitable position (profitLoss > 0)
     * LOSS = losing position    (profitLoss < 0)
     * NEUTRAL = breakeven or P&L unavailable
     */
    private String status;

    // ── Display ───────────────────────────────────────────────────────────
    private String title;
    private String description;
    private LocalDateTime timestamp;

    /** Resolve WIN / LOSS / NEUTRAL from an absolute P&L value. */
    public static String resolveStatus(Double profitLoss) {
        if (profitLoss == null) return "NEUTRAL";
        if (profitLoss > 0) return "WIN";
        if (profitLoss < 0) return "LOSS";
        return "NEUTRAL";
    }
}
