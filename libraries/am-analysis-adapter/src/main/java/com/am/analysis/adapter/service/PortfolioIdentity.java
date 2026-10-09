package com.am.analysis.adapter.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.model.components.HoldingIdentity;
import com.am.analysis.adapter.model.components.InvestmentStats;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Logical portfolio uniqueness for dashboard/analysis.
 * <p>
 * Distinct brokers (Groww, Upstox, …) stay separate. Re-uploading the same book
 * (same broker, or same display name) collapses to the latest entity — matching
 * trade-management's owner + case-insensitive name remap.
 */
public final class PortfolioIdentity {

    private PortfolioIdentity() {
    }

    /**
     * Stable identity key, or null when neither broker nor name is available.
     * Prefer broker (so GROW/GROWW collide); fall back to display name.
     */
    public static String keyOf(String brokerType, String portfolioName) {
        String broker = canonicalizeBroker(brokerType);
        if (broker != null) {
            return "broker:" + broker;
        }
        if (portfolioName != null && !portfolioName.isBlank()) {
            return "name:" + portfolioName.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    public static String keyOf(AnalysisEntity entity) {
        if (entity == null) {
            return null;
        }
        return keyOf(entity.getBrokerType(), entity.getPortfolioName());
    }

    /**
     * GROW and GROWW (and display "Groww") are one broker identity.
     */
    public static String canonicalizeBroker(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        // Enum GROW (legacy) and GROWW, plus display "Groww" → one identity
        if ("GROW".equals(normalized) || "GROWW".equals(normalized)) {
            return "GROWW";
        }
        return normalized;
    }

    /**
     * Fingerprint of open positions — used only to collapse legacy analysis rows
     * that lack name/broker metadata (pre-fix orphans from double upload).
     */
    public static String holdingsFingerprint(AnalysisEntity entity) {
        if (entity == null || entity.getHoldings() == null || entity.getHoldings().isEmpty()) {
            return null;
        }
        return entity.getHoldings().stream()
                .filter(Objects::nonNull)
                .map(PortfolioIdentity::holdingKey)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private static String holdingKey(AnalysisHolding holding) {
        HoldingIdentity id = holding.getIdentity();
        InvestmentStats inv = holding.getInvestment();
        String symbol = id != null && id.getSymbol() != null ? id.getSymbol().trim().toUpperCase(Locale.ROOT) : "";
        if (symbol.isEmpty() && id != null && id.getIsin() != null) {
            symbol = id.getIsin().trim().toUpperCase(Locale.ROOT);
        }
        if (symbol.isEmpty()) {
            return null;
        }
        double qty = inv != null && inv.getQuantity() != null ? inv.getQuantity() : 0.0;
        return symbol + "=" + qty;
    }

    /**
     * Keep one entity per logical portfolio: prefer broker/name key; for legacy
     * rows without metadata, collapse identical holdings fingerprints.
     * Within a group, keep the newest {@code lastUpdated} (nulls last).
     */
    public static List<AnalysisEntity> collapseToLatest(List<AnalysisEntity> entities) {
        if (entities == null || entities.size() <= 1) {
            return entities;
        }

        Map<String, AnalysisEntity> byIdentity = new LinkedHashMap<>();
        for (AnalysisEntity entity : entities) {
            if (entity == null) {
                continue;
            }
            String key = keyOf(entity);
            if (key == null) {
                String fp = holdingsFingerprint(entity);
                key = fp != null ? "holdings:" + fp : "id:" + entity.getId();
            }
            AnalysisEntity existing = byIdentity.get(key);
            if (existing == null || isNewer(entity, existing)) {
                byIdentity.put(key, entity);
            }
        }
        return List.copyOf(byIdentity.values());
    }

    public static boolean isNewer(AnalysisEntity candidate, AnalysisEntity incumbent) {
        if (candidate.getLastUpdated() == null) {
            return false;
        }
        if (incumbent.getLastUpdated() == null) {
            return true;
        }
        return candidate.getLastUpdated().isAfter(incumbent.getLastUpdated());
    }

    public static Comparator<AnalysisEntity> newestFirst() {
        return Comparator.comparing(AnalysisEntity::getLastUpdated,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
