package com.am.observability.http;

import com.am.observability.kafka.KafkaTraceHeaders;
import com.am.observability.mdc.MdcKeys;
import com.am.observability.trace.TracingHelper;
import org.slf4j.MDC;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

/**
 * Intended to be plugged into the OpenAPI-generated {@code ApiClient} classes
 * (in {@code am-portfolio-client-lib}, {@code am-market-client-lib},
 * {@code am-trade-client-lib}, {@code am-analysis-client-lib}) which expose
 * a {@code setRequestInterceptor(Consumer<HttpRequest.Builder>)} hook.
 *
 * <p>Injects {@code traceparent}, {@code X-Correlation-Id}, and
 * {@code X-User-Id} on every outbound request so the downstream service joins
 * the same trace and emits consistent logs.</p>
 */
public class TraceContextSdkInterceptor implements Consumer<HttpRequest.Builder> {

    private final TracingHelper tracingHelper;

    public TraceContextSdkInterceptor(TracingHelper tracingHelper) {
        this.tracingHelper = tracingHelper;
    }

    @Override
    public void accept(HttpRequest.Builder builder) {
        String traceId = tracingHelper.currentTraceIdOrNull();
        String spanId = tracingHelper.currentSpanIdOrNull();
        if (traceId != null && spanId != null) {
            builder.header(KafkaTraceHeaders.TRACEPARENT, w3cTraceparent(traceId, spanId));
        }
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null) {
            builder.header(KafkaTraceHeaders.CORRELATION_ID, correlationId);
        }
        String userId = MDC.get(MdcKeys.USER_ID);
        if (userId != null) {
            builder.header(KafkaTraceHeaders.USER_ID, userId);
        }
    }

    private static String w3cTraceparent(String traceId, String spanId) {
        return "00-" + pad(traceId, 32) + "-" + pad(spanId, 16) + "-01";
    }

    private static String pad(String hex, int len) {
        if (hex == null) {
            return "0".repeat(len);
        }
        if (hex.length() == len) {
            return hex;
        }
        if (hex.length() > len) {
            return hex.substring(0, len);
        }
        StringBuilder sb = new StringBuilder(len);
        for (int i = hex.length(); i < len; i++) {
            sb.append('0');
        }
        sb.append(hex);
        return sb.toString();
    }
}
