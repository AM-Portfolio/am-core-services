package com.am.observability.kafka;

/**
 * Kafka record header names used to carry trace context across services.
 */
public final class KafkaTraceHeaders {

    private KafkaTraceHeaders() {
    }

    /**
     * W3C traceparent header. Mirrors what Micrometer/OTel auto-instrumentation
     * adds; kept as an explicit constant so manual producer paths can stamp it
     * consistently.
     */
    public static final String TRACEPARENT = "traceparent";

    /**
     * Legacy correlation header used by GatewayKafkaProducer.
     */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    /**
     * Optional user identifier propagated for log enrichment on consumers.
     */
    public static final String USER_ID = "X-User-Id";
}
