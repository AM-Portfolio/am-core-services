package com.am.observability.web;

import com.am.observability.mdc.MdcKeys;
import com.am.observability.trace.TracingHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

/**
 * Populates MDC with {@code traceId}, {@code spanId}, {@code service},
 * {@code correlationId}, {@code request.method}, {@code request.path},
 * and {@code userId}. Authenticated principals (including a Spring Security
 * JWT's {@code sub}) take precedence over the legacy {@code X-User-Id}
 * propagation header.
 *
 * <p>Runs <em>after</em> Spring Boot's auto-instrumented tracing filter so
 * that {@code tracer.currentSpan()} is already populated.</p>
 *
 * <p>Falls back to legacy correlation headers when no {@code traceparent}
 * is supplied by the client: {@code X-Correlation-Id} or {@code X-Request-Id}.
 * If neither is set, generates a UUID.</p>
 */
public class TraceContextFilter extends OncePerRequestFilter implements Ordered {

    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_USER_ID = "X-User-Id";

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 50;

    private final TracingHelper tracingHelper;
    private final String serviceName;

    public TraceContextFilter(TracingHelper tracingHelper, String serviceName) {
        this.tracingHelper = tracingHelper;
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean putService = MDC.get(MdcKeys.SERVICE) == null;
        boolean putTrace = false;
        boolean putSpan = false;
        boolean putCorrelation = false;
        boolean putUser = false;
        boolean putMethod = false;
        boolean putPath = false;

        try {
            if (putService && serviceName != null) {
                MDC.put(MdcKeys.SERVICE, serviceName);
            }

            String traceId = tracingHelper.currentTraceIdOrNull();
            if (traceId != null) {
                MDC.put(MdcKeys.TRACE_ID, traceId);
                putTrace = true;
            }
            String spanId = tracingHelper.currentSpanIdOrNull();
            if (spanId != null) {
                MDC.put(MdcKeys.SPAN_ID, spanId);
                putSpan = true;
            }

            String correlationId = firstNonBlank(
                    request.getHeader(HEADER_CORRELATION_ID),
                    request.getHeader(HEADER_REQUEST_ID),
                    traceId,
                    UUID.randomUUID().toString());
            MDC.put(MdcKeys.CORRELATION_ID, correlationId);
            putCorrelation = true;
            response.setHeader(HEADER_CORRELATION_ID, correlationId);

            String userId = resolveUserId(request);
            if (userId != null && !userId.isBlank()) {
                MDC.put(MdcKeys.USER_ID, userId);
                putUser = true;
            }

            MDC.put(MdcKeys.REQUEST_METHOD, request.getMethod());
            putMethod = true;
            MDC.put(MdcKeys.REQUEST_PATH, request.getRequestURI());
            putPath = true;

            chain.doFilter(request, response);
        } finally {
            if (putService) MDC.remove(MdcKeys.SERVICE);
            if (putTrace) MDC.remove(MdcKeys.TRACE_ID);
            if (putSpan) MDC.remove(MdcKeys.SPAN_ID);
            if (putCorrelation) MDC.remove(MdcKeys.CORRELATION_ID);
            if (putUser) MDC.remove(MdcKeys.USER_ID);
            if (putMethod) MDC.remove(MdcKeys.REQUEST_METHOD);
            if (putPath) MDC.remove(MdcKeys.REQUEST_PATH);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    static String resolveUserId(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        return firstNonBlank(request.getHeader(HEADER_USER_ID));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
