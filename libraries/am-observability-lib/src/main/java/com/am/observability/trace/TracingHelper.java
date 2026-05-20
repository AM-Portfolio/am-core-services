package com.am.observability.trace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Small helper for code that needs to read the current {@code traceId} /
 * {@code spanId} without depending on Micrometer types directly.
 *
 * <p>Prefer this over {@code UUID.randomUUID()} when populating
 * {@code traceId} / {@code spanId} fields inside Kafka event payloads so
 * the payload IDs match what Micrometer puts in MDC and on the wire.</p>
 */
public final class TracingHelper {

    private final Tracer tracer;

    public TracingHelper(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Returns the current trace id (32-char hex) or a fresh UUID-derived
     * fallback if there is no active span. Never returns {@code null}.
     */
    public String currentTraceIdOrNew() {
        String fromTracer = readTraceId();
        if (fromTracer != null && !fromTracer.isEmpty()) {
            return fromTracer;
        }
        String mdc = MDC.get("traceId");
        if (mdc != null && !mdc.isEmpty()) {
            return mdc;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Returns the current span id (16-char hex) or a fresh fallback if no
     * span is active. Never returns {@code null}.
     */
    public String currentSpanIdOrNew() {
        String fromTracer = readSpanId();
        if (fromTracer != null && !fromTracer.isEmpty()) {
            return fromTracer;
        }
        String mdc = MDC.get("spanId");
        if (mdc != null && !mdc.isEmpty()) {
            return mdc;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public String currentTraceIdOrNull() {
        return readTraceId();
    }

    public String currentSpanIdOrNull() {
        return readSpanId();
    }

    private String readTraceId() {
        if (tracer == null) {
            return null;
        }
        try {
            Span current = tracer.currentSpan();
            if (current != null) {
                TraceContext context = current.context();
                if (context != null) {
                    return context.traceId();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String readSpanId() {
        if (tracer == null) {
            return null;
        }
        try {
            Span current = tracer.currentSpan();
            if (current != null) {
                TraceContext context = current.context();
                if (context != null) {
                    return context.spanId();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
