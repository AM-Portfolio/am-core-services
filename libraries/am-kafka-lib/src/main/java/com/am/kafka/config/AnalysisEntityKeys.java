package com.am.kafka.config;

/**
 * Layer 2 — MongoDB {@code AnalysisEntity} identifiers for aggregated (global) portfolio views.
 */
public final class AnalysisEntityKeys {

    public static final String GLOBAL_SOURCE_ID = "GLOBAL";

    private AnalysisEntityKeys() {}

    public static String globalEntityId(String userId) {
        return "PORTFOLIO_GLOBAL_" + userId;
    }

    public static boolean isGlobalSourceId(String sourceId) {
        return GLOBAL_SOURCE_ID.equals(sourceId);
    }

    /** Mongo {@code AnalysisEntity.id} for a portfolio watch target. */
    public static String portfolioEntityId(String portfolioId, String userId) {
        if (portfolioId == null || portfolioId.isBlank() || isGlobalSourceId(portfolioId)) {
            return globalEntityId(userId);
        }
        return "PORTFOLIO_" + portfolioId;
    }
}
