package com.am.observability.web;

import com.am.observability.config.ObservabilityProperties;
import com.am.observability.mdc.MdcKeys;
import com.am.observability.sanitize.Sanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Emits exactly one structured INFO line per HTTP request with method, path,
 * status, duration, and optionally sanitised payload. Skips paths configured under
 * {@code am.observability.request-log.ignore-paths}.
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = TraceContextFilter.ORDER + 10;
    private static final int MAX_PAYLOAD_LIMIT = 10240; // 10KB Safeguard limit

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> ignorePaths;
    private final boolean logPayloads;
    private final Sanitizer sanitizer;

    public RequestLoggingFilter(ObservabilityProperties.RequestLog config, Sanitizer sanitizer) {
        this.ignorePaths = config.getIgnorePaths();
        this.logPayloads = config.isLogPayloads();
        this.sanitizer = sanitizer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (shouldIgnore(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest requestToUse = request;
        HttpServletResponse responseToUse = response;

        if (logPayloads) {
            if (!(request instanceof ContentCachingRequestWrapper)) {
                requestToUse = new ContentCachingRequestWrapper(request);
            }
            if (!(response instanceof ContentCachingResponseWrapper)) {
                responseToUse = new ContentCachingResponseWrapper(response);
            }
        }

        long start = System.nanoTime();
        int status = 0;
        Throwable failure = null;
        try {
            chain.doFilter(requestToUse, responseToUse);
            status = responseToUse.getStatus();
        } catch (Throwable t) {
            failure = t;
            status = responseToUse.getStatus();
            if (status == 200) {
                status = 500;
            }
            throw t;
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            MDC.put(MdcKeys.HTTP_STATUS, String.valueOf(status));
            try {
                String method = requestToUse.getMethod();
                String userId = MDC.get(MdcKeys.USER_ID);
                if (userId == null || userId.isBlank()) {
                    Object attr = requestToUse.getAttribute("am.observability.userId");
                    if (attr instanceof String s && !s.isBlank()) {
                        userId = s;
                    }
                }
                String query = requestToUse.getQueryString();
                String queryFragment = query == null ? "" : "?" + query;
                String userFragment = userId == null ? "" : " user=" + userId;

                String payloadFragment = "";
                if (logPayloads) {
                    String reqPayload = getPayloadAsString(requestToUse);
                    String respPayload = getPayloadAsString(responseToUse);
                    payloadFragment = String.format(" reqPayload=[%s] respPayload=[%s]", reqPayload, respPayload);
                }

                if (failure == null) {
                    log.info("HTTP {} {}{} -> {} ({} ms){}{}", method, path, queryFragment, status, elapsedMs, userFragment, payloadFragment);
                } else {
                    log.error("HTTP {} {}{} -> {} ({} ms){} cause={}{}", method, path, queryFragment, status, elapsedMs, userFragment, failure.getClass().getSimpleName(), payloadFragment);
                }
            } finally {
                MDC.remove(MdcKeys.HTTP_STATUS);
                if (logPayloads && responseToUse instanceof ContentCachingResponseWrapper responseWrapper) {
                    responseWrapper.copyBodyToResponse();
                }
            }
        }
    }

    private String getPayloadAsString(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            return extractAndSanitize(buf);
        }
        return "N/A";
    }

    private String getPayloadAsString(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            return extractAndSanitize(buf);
        }
        return "N/A";
    }

    private String extractAndSanitize(byte[] buf) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        int length = Math.min(buf.length, MAX_PAYLOAD_LIMIT);
        String raw = new String(buf, 0, length, StandardCharsets.UTF_8);
        String sanitized = sanitizer.maskJson(raw);
        if (buf.length > MAX_PAYLOAD_LIMIT) {
            sanitized += " [Payload truncated - exceeds 10KB]";
        }
        return sanitized;
    }

    private boolean shouldIgnore(String path) {
        if (path == null || ignorePaths == null || ignorePaths.isEmpty()) {
            return false;
        }
        for (String pattern : ignorePaths) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
