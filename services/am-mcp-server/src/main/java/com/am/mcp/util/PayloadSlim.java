package com.am.mcp.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat-safe payload shaping: whitelist keys and cap list length under MCP max chars.
 */
public final class PayloadSlim {

    public static final int HOLDINGS_LIMIT = 20;
    public static final int TRADE_LIMIT = 20;
    public static final int TRADE_HARD_MAX = 40;
    public static final int HISTORY_BARS = 60;
    public static final int SEARCH_LIMIT = 15;
    public static final int MOVERS_LIMIT = 15;
    public static final int BASKET_LIMIT = 20;

    private PayloadSlim() {
    }

    public static Map<String, Object> pick(Map<String, Object> raw, String... keys) {
        Map<String, Object> slim = new LinkedHashMap<>();
        if (raw == null) {
            return slim;
        }
        for (String key : keys) {
            if (raw.containsKey(key) && raw.get(key) != null) {
                slim.put(key, raw.get(key));
            }
        }
        return slim;
    }

    public static Map<String, Object> pickLoose(Object raw, String... keys) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null) {
                    typed.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
            return pick(typed, keys);
        }
        return new LinkedHashMap<>();
    }

    /**
     * Cap a list of maps to {@code limit}, keeping only {@code rowKeys} per row.
     * Result: {listKey: [...], count: originalSize, truncated: bool}.
     */
    public static Map<String, Object> mapList(
            Object rawList,
            String listKey,
            int limit,
            String... rowKeys) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(rawList instanceof List<?> list)) {
            out.put("count", 0);
            out.put("truncated", false);
            out.put(listKey, List.of());
            return out;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> row = pickLoose(item, rowKeys);
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        boolean truncated = rows.size() > limit;
        out.put("count", rows.size());
        out.put("truncated", truncated);
        out.put(listKey, truncated ? List.copyOf(rows.subList(0, limit)) : List.copyOf(rows));
        return out;
    }

    /** Keep the last {@code limit} items of a list (for time series). */
    public static Map<String, Object> lastN(
            Object rawList,
            String listKey,
            int limit,
            String... rowKeys) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(rawList instanceof List<?> list)) {
            out.put("count", 0);
            out.put("truncated", false);
            out.put(listKey, List.of());
            return out;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> row = pickLoose(item, rowKeys);
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        boolean truncated = rows.size() > limit;
        List<Map<String, Object>> kept = truncated
                ? rows.subList(rows.size() - limit, rows.size())
                : rows;
        out.put("count", rows.size());
        out.put("truncated", truncated);
        out.put(listKey, List.copyOf(kept));
        return out;
    }

    public static int clampLimit(Integer limit, int defaultLimit, int hardMax) {
        if (limit == null || limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, hardMax);
    }

    public static double asDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    /** Slim filter/date-range style Map responses that may nest content lists. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> slimNestedListPayload(Object raw, int limit, String... rowKeys) {
        if (!(raw instanceof Map<?, ?> map)) {
            return mapList(raw, "items", limit, rowKeys);
        }
        Map<String, Object> typed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() != null) {
                typed.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        for (String candidate : List.of("content", "trades", "activities", "items", "data")) {
            Object list = typed.get(candidate);
            if (list instanceof List<?>) {
                Map<String, Object> slim = mapList(list, candidate, limit, rowKeys);
                for (String keep : List.of("page", "totalElements", "totalPages", "size")) {
                    if (typed.containsKey(keep)) {
                        slim.put(keep, typed.get(keep));
                    }
                }
                return slim;
            }
        }
        return pick(typed,
                "winRate", "totalTrades", "totalPnl", "averagePnl", "profitFactor",
                "totalInvested", "currentValue", "unrealisedPnL", "realizedPnl",
                "count", "symbol", "status", "portfolioId");
    }

    public static Map<String, Object> slimBasket(Object raw, int limit) {
        if (!(raw instanceof Map<?, ?> map)) {
            if (raw instanceof List<?> list) {
                return mapList(list, "items", limit,
                        "symbol", "name", "isin", "weight", "quantity", "value", "etfIsin", "score");
            }
            Map<String, Object> wrap = new LinkedHashMap<>();
            wrap.put("value", raw);
            return wrap;
        }
        Map<String, Object> typed = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() != null) {
                typed.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
        Map<String, Object> slim = new LinkedHashMap<>();
        for (String key : List.of(
                "portfolioId", "etfIsin", "etfName", "score", "overlap",
                "investmentAmount", "totalQuantity", "estimatedCost")) {
            if (typed.containsKey(key)) {
                slim.put(key, typed.get(key));
            }
        }
        for (String listKey : List.of(
                "opportunities", "holdings", "allocations", "exposure",
                "composition", "quantities", "items", "stocks", "sectors")) {
            Object list = typed.get(listKey);
            if (list instanceof List<?>) {
                Map<String, Object> part = mapList(list, listKey, limit,
                        "symbol", "name", "isin", "weight", "quantity", "value",
                        "sector", "percentage", "score", "overlap", "price");
                slim.putAll(part);
                return slim;
            }
            if (list instanceof Map<?, ?> nested) {
                slim.put(listKey, pickLoose(nested,
                        "symbol", "name", "value", "percentage", "weight", "count"));
            }
        }
        if (slim.isEmpty()) {
            return pick(typed, typed.keySet().stream().limit(12).toArray(String[]::new));
        }
        return slim;
    }
}
