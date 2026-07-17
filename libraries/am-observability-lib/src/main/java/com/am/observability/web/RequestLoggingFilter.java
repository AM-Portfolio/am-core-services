package com.am.observability.web;

import com.am.observability.config.ObservabilityProperties;
import com.am.observability.mdc.MdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Emits exactly one structured INFO line per HTTP request with method, path,
 * status, duration and (sanitised) user. Skips paths configured under
 * {@code am.observability.request-log.ignore-paths}.
 *
 * <p>Runs after {@link TraceContextFilter} so MDC is already populated.</p>
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = TraceContextFilter.ORDER + 10;

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> ignorePaths;

    public RequestLoggingFilter(ObservabilityProperties.RequestLog config) {
        this.ignorePaths = config.getIgnorePaths();
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

        long start = System.nanoTime();
        int status = 0;
        Throwable failure = null;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } catch (Throwable t) {
            failure = t;
            status = response.getStatus();
            if (status == 200) {
                status = 500;
            }
            throw t;
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            MDC.put(MdcKeys.HTTP_STATUS, String.valueOf(status));
            try {
                String method = request.getMethod();
                String userId = MDC.get(MdcKeys.USER_ID);
                if (userId == null || userId.isBlank()) {
                    Object attr = request.getAttribute("am.observability.userId");
                    if (attr instanceof String s && !s.isBlank()) {
                        userId = s;
                    }
                }
                String query = request.getQueryString();
                String queryFragment = query == null ? "" : "?" + query;
                String userFragment = userId == null ? "" : " user=" + userId;
                if (failure == null) {
                    log.info("HTTP {} {}{} -> {} ({} ms){}", method, path, queryFragment, status, elapsedMs, userFragment);
                } else {
                    log.error("HTTP {} {}{} -> {} ({} ms){} cause={}", method, path, queryFragment, status, elapsedMs, userFragment, failure.getClass().getSimpleName());
                }
            } finally {
                MDC.remove(MdcKeys.HTTP_STATUS);
            }
        }
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
