package com.am.kafka.config;

/**
 * Redis key prefixes for shared market / dashboard data.
 * Window identifiers: use {@link Timeframe} (not string literals).
 */
public final class MarketDataKeys {

    private MarketDataKeys() {
    }

    public static final String PREV_CLOSE_PREFIX = "prev-close:";
    public static final String DASHBOARD_SNAPSHOT_PREFIX = "dashboard:snapshot:";
}
