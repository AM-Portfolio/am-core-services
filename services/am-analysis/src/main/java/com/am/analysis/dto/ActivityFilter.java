package com.am.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filter parameters for the Recent Activity endpoint.
 * All fields are optional — omit to get unfiltered results.
 *
 * Usage (query params):
 *   GET /api/v1/analysis/dashboard/recent-activity
 *       ?userId=xxx
 *       &type=HOLDING
 *       &status=WIN
 *       &sector=Technology
 *       &sortBy=PROFIT_LOSS
 *       &page=0
 *       &size=20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityFilter {

    /** Filter by activity type: HOLDING, PORTFOLIO_UPDATE, ALERT */
    private String type;

    /** Filter by P&L status: WIN, LOSS, NEUTRAL */
    private String status;

    /** Filter by sector: e.g. "Technology", "Finance" */
    private String sector;

    /** Filter by portfolio name (partial match) */
    private String portfolioName;

    /**
     * Sort field. Values:
     *   TIMESTAMP        → most recently updated first (default)
     *   PROFIT_LOSS      → biggest winners first
     *   PROFIT_LOSS_ASC  → biggest losers first
     *   DAY_CHANGE       → best intraday performers first
     *   CURRENT_VALUE    → highest value positions first
     */
    @Builder.Default
    private String sortBy = "TIMESTAMP";

    /** Zero-based page number */
    @Builder.Default
    private int page = 0;

    /** Max items per page (default 20, max 100) */
    @Builder.Default
    private int size = 20;
}
