package com.am.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated wrapper for recent activity results.
 *
 * Includes summary counters for quick overview without full list traversal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {

    private List<ActivityItem> items;

    // Pagination metadata
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    // Quick summary counters (pre-computed, don't require full list scan on client)
    private int totalWinning;
    private int totalLosing;
    private int totalNeutral;
}
