package com.am.observability.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds {@code am.observability.*} properties. All knobs default to sensible
 * production values; tune via environment variables.
 */
@Data
@ConfigurationProperties(prefix = "am.observability")
public class ObservabilityProperties {

    /**
     * Master switch. When false, the auto-config backs out completely and no
     * filters / interceptors are registered. Default true.
     */
    private boolean enabled = true;

    /**
     * Request logging settings.
     */
    private RequestLog requestLog = new RequestLog();

    /**
     * Sanitiser settings.
     */
    private Sanitize sanitize = new Sanitize();

    /**
     * Micrometer meter rename map (local name → canonical Micrometer name).
     */
    private Metrics metrics = new Metrics();

    @Data
    public static class RequestLog {
        /**
         * Emit a structured access log per HTTP request. Default true.
         */
        private boolean enabled = true;

        /**
         * Log the HTTP JSON request and response payloads. Default false.
         */
        private boolean logPayloads = false;

        /**
         * Ant-style paths to skip in the access log. Default skips actuator.
         */
        private List<String> ignorePaths = new ArrayList<>(List.of(
                "/actuator/**",
                "/favicon.ico",
                "/swagger-ui/**",
                "/api-docs/**",
                "/v3/api-docs/**"
        ));
    }

    @Data
    public static class Sanitize {
        /**
         * Max number of characters in payload preview emitted at DEBUG.
         */
        private int previewBytes = 256;

        /**
         * Additional field names to mask, on top of the built-in list.
         */
        private List<String> extraFields = new ArrayList<>();
    }

    @Data
    public static class Metrics {
        /**
         * Exact Micrometer meter-name remaps (local → canonical). Empty = no-op.
         * Example: {@code abc: websocket.sessions.active}
         * Use Micrometer dotted names, not Prometheus underscores.
         */
        private Map<String, String> map = new LinkedHashMap<>();
    }
}
