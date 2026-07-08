package com.am.analysis.adapter.model;

/**
 * Documents the JSON payload contract for each {@link DashboardSnapshot} widget.
 * Payload types live in {@code com.am.analysis.dto} (am-analysis module).
 */
public final class DashboardSnapshotContract {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private DashboardSnapshotContract() {
    }

    /**
     * Fully-qualified payload type name for API/docs consumers.
     */
    public static String payloadTypeName(DashboardWidgetType widget) {
        return switch (widget) {
            case SUMMARY -> "com.am.analysis.dto.DashboardSummary";
            case ACTIVITY -> "com.am.analysis.dto.RecentActivityResponse";
            case ALLOCATION -> "com.am.analysis.dto.AllocationResponse";
            case MOVERS -> "com.am.analysis.dto.TopMoversResponse";
            case HISTORY -> "com.am.analysis.dto.PerformanceResponse";
        };
    }
}
