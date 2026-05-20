package com.am.observability.kafka;

import com.am.observability.mdc.MdcKeys;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Safety net interceptor that stamps trace headers on every outbound Kafka
 * record based on the current MDC. Spring Boot's auto-instrumentation already
 * stamps {@code traceparent} on {@link org.springframework.kafka.core.KafkaTemplate}
 * sends, but this interceptor guarantees coverage for raw {@link ProducerRecord}
 * paths (such as the manual {@code ProducerRecord} construction in
 * {@code GatewayKafkaProducer}) and also adds {@code X-Correlation-Id} +
 * {@code X-User-Id} for human readability on consumers.
 */
public class TracingKafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        if (record == null) {
            return null;
        }
        Headers headers = record.headers();
        addIfAbsent(headers, KafkaTraceHeaders.CORRELATION_ID, MDC.get(MdcKeys.CORRELATION_ID));
        addIfAbsent(headers, KafkaTraceHeaders.USER_ID, MDC.get(MdcKeys.USER_ID));
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // no-op
    }

    private static void addIfAbsent(Headers headers, String key, String value) {
        if (value == null || value.isEmpty() || headers == null) {
            return;
        }
        if (headers.lastHeader(key) == null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
