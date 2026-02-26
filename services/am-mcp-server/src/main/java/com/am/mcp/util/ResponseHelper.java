package com.am.mcp.util;

import com.am.mcp.config.AmMcpProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utility: truncate tool responses to protect LLM context window.
 *
 * MCP tools must always return String. Large JSON dumps hurt response quality
 * because they consume context and bury the relevant data points.
 * Configured by am.mcp.max-response-chars in application.yaml.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseHelper {

    private final ObjectMapper objectMapper;
    private final AmMcpProperties props;

    /**
     * Serialize object to JSON, then truncate if over the configured limit.
     * The truncation suffix tells Claude to request more specific data.
     */
    public String toJson(Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return truncate(json);
        } catch (Exception e) {
            return errorJson("serialization", e);
        }
    }

    /**
     * Truncate an already-serialized JSON string.
     */
    public String truncate(String json) {
        int max = props.getMcp().getMaxResponseChars();
        if (json.length() <= max) {
            return json;
        }
        log.debug("Response truncated from {} to {} chars", json.length(), max);
        return json.substring(0, max)
                + " ... [TRUNCATED: use more specific filters to narrow results]\"";
    }

    /**
     * Standard error response — never throw from a tool.
     */
    public String errorJson(String tool, Exception e) {
        return String.format("{\"error\":\"%s failed\",\"detail\":\"%s\"}",
                tool, e.getMessage() != null
                        ? e.getMessage().replace("\"", "'").substring(0, Math.min(200, e.getMessage().length()))
                        : "unknown error");
    }

    /**
     * Service-unavailable message returned by circuit-breaker fallbacks.
     */
    public String unavailable(String service) {
        return String.format(
                "{\"error\":\"%s is temporarily unavailable\",\"retry\":true,\"hint\":\"Try again in 30 seconds.\"}",
                service);
    }
}
