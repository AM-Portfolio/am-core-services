package com.am.mcp.util;

import com.am.mcp.config.AmMcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility: serialize tool responses and emit a consistent error envelope.
 * MCP tools must always return String — never throw from {@code @Tool} methods.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseHelper {

    private final ObjectMapper objectMapper;
    private final AmMcpProperties props;

    public String toJson(Object data) {
        try {
            return truncate(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            return failure("serialization", "SERIALIZATION_ERROR", e.getMessage(), false, null);
        }
    }

    public String success(Object data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("ok", true);
        envelope.put("data", data);
        return toJson(envelope);
    }

    public String truncate(String json) {
        int max = props.getMcp().getMaxResponseChars();
        if (json.length() <= max) {
            return json;
        }
        log.debug("Response truncated from {} to {} chars", json.length(), max);
        return json.substring(0, max)
                + " ... [TRUNCATED: use more specific filters to narrow results]\"";
    }

    public String errorJson(String tool, Exception e) {
        return failure(tool, "TOOL_FAILED",
                e.getMessage() != null ? e.getMessage() : "unknown error",
                true, "Retry the tool or use ask_finance_agent for a multi-step answer.");
    }

    public String unavailable(String service) {
        return failure(service, "SERVICE_UNAVAILABLE",
                service + " is temporarily unavailable",
                true, "Try again in 30 seconds.");
    }

    public String failure(String tool, String code, String message, boolean retry, String hint) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("ok", false);
        envelope.put("error", code);
        envelope.put("message", sanitize(message));
        envelope.put("retry", retry);
        envelope.put("tool", tool);
        if (hint != null && !hint.isBlank()) {
            envelope.put("hint", hint);
        }
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"ENVELOPE_FAILED\",\"message\":\""
                    + sanitize(message) + "\",\"retry\":true,\"tool\":\"" + tool + "\"}";
        }
    }

    private static String sanitize(String msg) {
        if (msg == null) {
            return "unknown error";
        }
        String cleaned = msg.replace("\"", "'");
        return cleaned.substring(0, Math.min(200, cleaned.length()));
    }
}
