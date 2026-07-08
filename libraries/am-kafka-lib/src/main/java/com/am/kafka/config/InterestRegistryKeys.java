package com.am.kafka.config;

/**
 * Layer 1 — WebSocket watch channel identifiers (interest registry values).
 * Never a Mongo portfolio ID or REST API alias.
 */
public final class InterestRegistryKeys {

    public static final String CHANNEL_DASHBOARD_MAIN = "CHANNEL:DASHBOARD_MAIN";

    /** @deprecated Legacy registry value; treat as dashboard channel during migration. */
    public static final String LEGACY_DASHBOARD_ALL = "ALL";

    private InterestRegistryKeys() {}

    public static boolean isDashboardChannel(String watchTarget) {
        if (watchTarget == null || watchTarget.isBlank()) {
            return false;
        }
        return CHANNEL_DASHBOARD_MAIN.equals(watchTarget)
                || LEGACY_DASHBOARD_ALL.equals(watchTarget);
    }
}
