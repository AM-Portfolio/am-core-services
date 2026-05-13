package com.am.observability.sanitize;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility for masking sensitive fields and truncating oversized payloads
 * before they reach a log line. Treat as the single chokepoint for any
 * payload preview emitted at INFO/DEBUG.
 */
public final class Sanitizer {

    private static final String MASK = "***";
    private static final int DEFAULT_PREVIEW_BYTES = 256;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> DEFAULT_SENSITIVE_KEYS = unmodifiable(
            "password", "pwd", "secret", "token", "authorization", "auth",
            "apikey", "api_key", "apiKey", "x-api-key",
            "pan", "cvv", "account", "iban", "ssn",
            "creditCard", "credit_card", "cardNumber"
    );

    private final Set<String> sensitiveKeys;
    private final int previewBytes;

    public Sanitizer() {
        this(Collections.emptySet(), DEFAULT_PREVIEW_BYTES);
    }

    public Sanitizer(Set<String> extraSensitiveKeys, int previewBytes) {
        Set<String> all = new LinkedHashSet<>(DEFAULT_SENSITIVE_KEYS);
        if (extraSensitiveKeys != null) {
            for (String k : extraSensitiveKeys) {
                if (k != null) {
                    all.add(k.toLowerCase());
                }
            }
        }
        this.sensitiveKeys = Collections.unmodifiableSet(all);
        this.previewBytes = previewBytes <= 0 ? DEFAULT_PREVIEW_BYTES : previewBytes;
    }

    public int previewBytes() {
        return previewBytes;
    }

    /**
     * Return a short, masked, ASCII-safe preview of a raw string.
     * Suitable for logging at DEBUG.
     */
    public String preview(String raw) {
        if (raw == null) {
            return null;
        }
        String masked;
        String trimmed = raw.trim();
        if (looksLikeJson(trimmed)) {
            masked = maskJson(trimmed);
        } else {
            masked = raw;
        }
        if (masked.length() <= previewBytes) {
            return masked;
        }
        return masked.substring(0, previewBytes) + "...(truncated " + (masked.length() - previewBytes) + ")";
    }

    /**
     * Mask any sensitive fields inside a JSON document. If parsing fails,
     * returns the original string (which the caller should still truncate).
     */
    public String maskJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            maskNode(node);
            return MAPPER.writeValueAsString(node);
        } catch (Exception ignored) {
            return json;
        }
    }

    /**
     * Return a new map with sensitive values masked. Original is not mutated.
     */
    public Map<String, Object> mask(Map<String, Object> input) {
        if (input == null) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>(input.size());
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isSensitive(key)) {
                out.put(key, MASK);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) value;
                out.put(key, mask(nested));
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    public boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        return sensitiveKeys.contains(key.toLowerCase());
    }

    private void maskNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(fieldName -> {
            });
            // collect names first to avoid concurrent modification
            java.util.List<String> names = new java.util.ArrayList<>();
            obj.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (isSensitive(name)) {
                    obj.put(name, MASK);
                } else {
                    maskNode(obj.get(name));
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskNode(child);
            }
        }
    }

    private static boolean looksLikeJson(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        char first = s.charAt(0);
        return first == '{' || first == '[';
    }

    private static Set<String> unmodifiable(String... values) {
        Set<String> set = new LinkedHashSet<>();
        for (String v : values) {
            set.add(v.toLowerCase());
        }
        return Collections.unmodifiableSet(set);
    }
}
