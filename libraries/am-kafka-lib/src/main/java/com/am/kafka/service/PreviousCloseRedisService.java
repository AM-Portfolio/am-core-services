package com.am.kafka.service;

import com.am.kafka.config.MarketDataKeys;
import com.am.kafka.config.Timeframe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreviousCloseRedisService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration TTL = Duration.ofHours(48);

    public void write(String symbol, Map<String, Double> previousCloseValues) {
        if (symbol == null || previousCloseValues == null || previousCloseValues.isEmpty()) {
            return;
        }
        try {
            String key = redisKey(symbol);
            Map<String, String> stringMap = previousCloseValues.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.valueOf(e.getValue())
                    ));
            redisTemplate.opsForHash().putAll(key, stringMap);
            redisTemplate.expire(key, TTL);
            log.debug("[Redis] Saved previous close values for symbol {}: {}", symbol, stringMap);
        } catch (Exception ex) {
            log.warn("[Redis] Failed to write previous close values for {}: {}", symbol, ex.getMessage());
        }
    }

    /** Full hash for one symbol ({@code HGETALL}). */
    public Map<String, Double> read(String symbol) {
        if (symbol == null) {
            return Map.of();
        }
        Map<String, Map<String, Double>> batch = readForSymbols(List.of(symbol));
        return batch.getOrDefault(symbol, Map.of());
    }

    /**
     * Multiple hash fields for one symbol ({@code HMGET}).
     * Missing fields are omitted from the result.
     */
    public Map<Timeframe, Double> readWindows(String symbol, Collection<Timeframe> windows) {
        if (symbol == null || windows == null || windows.isEmpty()) {
            return Map.of();
        }
        List<Timeframe> windowList = distinctWindows(windows);
        if (windowList.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> fields = windowList.stream().map(Timeframe::getCode).toList();
            List<String> values = hashOps().multiGet(redisKey(symbol), fields);
            return zipWindows(windowList, values);
        } catch (Exception ex) {
            log.warn("[Redis] Failed HMGET previous close for {} windows {}: {}",
                    symbol, windowList, ex.getMessage());
            return Map.of();
        }
    }

    public Map<String, Double> readWindowCodes(String symbol, Collection<String> windowCodes) {
        if (symbol == null || windowCodes == null || windowCodes.isEmpty()) {
            return Map.of();
        }
        List<String> codes = windowCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (codes.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> values = hashOps().multiGet(redisKey(symbol), codes);
            Map<String, Double> result = new LinkedHashMap<>();
            for (int i = 0; i < codes.size(); i++) {
                Double parsed = parseDouble(values != null && i < values.size() ? values.get(i) : null);
                if (parsed != null) {
                    result.put(codes.get(i), parsed);
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("[Redis] Failed HMGET previous close for {} codes {}: {}", symbol, codes, ex.getMessage());
            return Map.of();
        }
    }

    public Double readWindow(String symbol, Timeframe window) {
        if (symbol == null || window == null) {
            return null;
        }
        Double fromHash = readWindows(symbol, List.of(window)).get(window);
        if (fromHash != null) {
            return fromHash;
        }
        if (window == Timeframe.ONE_DAY) {
            return readMarketStringPrevClose(symbol);
        }
        return null;
    }

    public Double readWindow(String symbol, String window) {
        if (symbol == null || window == null) {
            return null;
        }
        return readWindowCodes(symbol, List.of(window)).get(window);
    }

    /**
     * Same window for many symbols — pipelined {@code HMGET} on HASH keys, then pipelined
     * {@code GET} on {@code market:prev-close:*} for 1D HASH misses (two round-trips total).
     */
    public Map<String, Double> readWindowForSymbols(Collection<String> symbols, Timeframe window) {
        if (symbols == null || symbols.isEmpty() || window == null) {
            return Map.of();
        }
        List<String> symbolList = distinctSymbols(symbols);
        Map<String, Double> result = readWindowsForSymbols(symbolList, List.of(window)).entrySet().stream()
                .filter(e -> e.getValue().containsKey(window))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(window),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        if (window == Timeframe.ONE_DAY) {
            List<String> hashMisses = symbolList.stream()
                    .filter(symbol -> !result.containsKey(symbol))
                    .toList();
            if (!hashMisses.isEmpty()) {
                result.putAll(readMarketStringPrevCloseForSymbols(hashMisses));
            }
        }
        return result;
    }

    /**
     * Same set of windows for many symbols — pipelined {@code HMGET} per symbol.
     */
    public Map<String, Map<Timeframe, Double>> readWindowsForSymbols(
            Collection<String> symbols,
            Collection<Timeframe> windows) {
        List<String> symbolList = distinctSymbols(symbols);
        List<Timeframe> windowList = distinctWindows(windows);
        if (symbolList.isEmpty() || windowList.isEmpty()) {
            return Map.of();
        }

        List<String> fields = windowList.stream().map(Timeframe::getCode).toList();
        try {
            List<Object> pipelineResults = redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public Object execute(RedisOperations operations) throws DataAccessException {
                    HashOperations<String, String, String> hashOps = operations.opsForHash();
                    for (String symbol : symbolList) {
                        hashOps.multiGet(redisKey(symbol), fields);
                    }
                    return null;
                }
            });

            Map<String, Map<Timeframe, Double>> result = new LinkedHashMap<>();
            for (int i = 0; i < symbolList.size(); i++) {
                @SuppressWarnings("unchecked")
                List<String> values = pipelineResults.get(i) instanceof List<?> list
                        ? (List<String>) list
                        : List.of();
                Map<Timeframe, Double> windowsForSymbol = zipWindows(windowList, values);
                if (!windowsForSymbol.isEmpty()) {
                    result.put(symbolList.get(i), windowsForSymbol);
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("[Redis] Failed pipelined HMGET for {} symbols, {} windows: {}",
                    symbolList.size(), windowList.size(), ex.getMessage());
            return Map.of();
        }
    }

    /**
     * Full hash per symbol — pipelined {@code HGETALL}.
     */
    public Map<String, Map<String, Double>> readForSymbols(Collection<String> symbols) {
        List<String> symbolList = distinctSymbols(symbols);
        if (symbolList.isEmpty()) {
            return Map.of();
        }

        try {
            List<Object> pipelineResults = redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public Object execute(RedisOperations operations) throws DataAccessException {
                    HashOperations<String, String, String> hashOps = operations.opsForHash();
                    for (String symbol : symbolList) {
                        hashOps.entries(redisKey(symbol));
                    }
                    return null;
                }
            });

            Map<String, Map<String, Double>> result = new LinkedHashMap<>();
            for (int i = 0; i < symbolList.size(); i++) {
                Object raw = pipelineResults.get(i);
                if (!(raw instanceof Map<?, ?> entries) || entries.isEmpty()) {
                    continue;
                }
                Map<String, Double> parsed = entries.entrySet().stream()
                        .map(e -> Map.entry(e.getKey().toString(), parseDouble(e.getValue())))
                        .filter(e -> e.getValue() != null)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
                if (!parsed.isEmpty()) {
                    result.put(symbolList.get(i), parsed);
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("[Redis] Failed pipelined HGETALL for {} symbols: {}", symbolList.size(), ex.getMessage());
            return Map.of();
        }
    }

    private HashOperations<String, String, String> hashOps() {
        return redisTemplate.opsForHash();
    }

    private static String redisKey(String symbol) {
        return MarketDataKeys.PREV_CLOSE_PREFIX + symbol;
    }

    /**
     * Fallback: market-data stores 1D prev-close as a STRING at {@code market:prev-close:{symbol}}.
     * Tries symbol and NSE_EQ alias variants.
     */
    private Double readMarketStringPrevClose(String symbol) {
        return readMarketStringPrevCloseForSymbols(List.of(symbol)).get(symbol);
    }

    /**
     * Pipelined GET for {@code market:prev-close:*} keys — one round-trip for all HASH misses.
     */
    private Map<String, Double> readMarketStringPrevCloseForSymbols(Collection<String> symbols) {
        List<String> symbolList = distinctSymbols(symbols);
        if (symbolList.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> candidatesBySymbol = new LinkedHashMap<>();
        List<String> uniqueKeys = new ArrayList<>();
        var keyIndex = new LinkedHashMap<String, Integer>();

        for (String symbol : symbolList) {
            List<String> candidates = marketRedisKeyCandidates(symbol);
            candidatesBySymbol.put(symbol, candidates);
            for (String key : candidates) {
                if (!keyIndex.containsKey(key)) {
                    keyIndex.put(key, uniqueKeys.size());
                    uniqueKeys.add(key);
                }
            }
        }
        if (uniqueKeys.isEmpty()) {
            return Map.of();
        }

        Map<String, Double> valuesByKey;
        try {
            List<Object> pipelineResults = redisTemplate.executePipelined(new SessionCallback<>() {
                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (String key : uniqueKeys) {
                        operations.opsForValue().get(key);
                    }
                    return null;
                }
            });

            valuesByKey = new LinkedHashMap<>();
            for (int i = 0; i < uniqueKeys.size(); i++) {
                Double parsed = parseDouble(pipelineResults.get(i));
                if (parsed != null) {
                    valuesByKey.put(uniqueKeys.get(i), parsed);
                }
            }
        } catch (Exception ex) {
            log.warn("[Redis] Failed pipelined GET market prev-close for {} symbols ({} keys): {}",
                    symbolList.size(), uniqueKeys.size(), ex.getMessage());
            return Map.of();
        }

        Map<String, Double> result = new LinkedHashMap<>();
        for (String symbol : symbolList) {
            for (String key : candidatesBySymbol.get(symbol)) {
                Double value = valuesByKey.get(key);
                if (value != null) {
                    log.debug("[Redis] Market STRING prev-close hit for {} via key {}", symbol, key);
                    result.put(symbol, value);
                    break;
                }
            }
        }
        return result;
    }

    static List<String> marketRedisKeyCandidates(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return List.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        String trimmed = symbol.trim();
        keys.add(MarketDataKeys.MARKET_PREV_CLOSE_PREFIX + trimmed);
        String base = baseSymbol(trimmed);
        if (!base.isEmpty() && !base.equals(trimmed)) {
            keys.add(MarketDataKeys.MARKET_PREV_CLOSE_PREFIX + base);
        }
        if (!trimmed.startsWith("NSE_EQ:") && !base.isEmpty()) {
            keys.add(MarketDataKeys.MARKET_PREV_CLOSE_PREFIX + "NSE_EQ:" + base);
        }
        return new ArrayList<>(keys);
    }

    private static String baseSymbol(String symbol) {
        String normalized = symbol.trim();
        if (normalized.contains("|")) {
            normalized = normalized.substring(normalized.lastIndexOf('|') + 1);
        }
        if (normalized.contains(":")) {
            normalized = normalized.substring(normalized.lastIndexOf(':') + 1);
        }
        return normalized.trim();
    }

    private static List<String> distinctSymbols(Collection<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }
        return symbols.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private static List<Timeframe> distinctWindows(Collection<Timeframe> windows) {
        if (windows == null || windows.isEmpty()) {
            return List.of();
        }
        return windows.stream().filter(Objects::nonNull).distinct().toList();
    }

    private static Map<Timeframe, Double> zipWindows(List<Timeframe> windows, List<String> values) {
        Map<Timeframe, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < windows.size(); i++) {
            Double parsed = parseDouble(values != null && i < values.size() ? values.get(i) : null);
            if (parsed != null) {
                result.put(windows.get(i), parsed);
            }
        }
        return result;
    }

    private static Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
