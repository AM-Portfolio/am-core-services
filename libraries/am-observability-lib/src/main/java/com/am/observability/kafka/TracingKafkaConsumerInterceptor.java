package com.am.observability.kafka;

import com.am.observability.mdc.MdcKeys;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Safety-net interceptor that copies legacy correlation / user headers from
 * the first record of each poll into MDC so consumer logs are enriched even
 * when Spring's Kafka instrumentation has not yet executed.
 *
 * <p>Note: this is best-effort enrichment; Micrometer's Kafka instrumentation
 * is the primary mechanism for full distributed tracing on consume.</p>
 */
public class TracingKafkaConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        if (records == null || records.isEmpty()) {
            return records;
        }
        ConsumerRecord<Object, Object> first = records.iterator().next();
        copyHeaderToMdc(first, KafkaTraceHeaders.CORRELATION_ID, MdcKeys.CORRELATION_ID);
        copyHeaderToMdc(first, KafkaTraceHeaders.USER_ID, MdcKeys.USER_ID);
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
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

    private static void copyHeaderToMdc(ConsumerRecord<?, ?> record, String headerKey, String mdcKey) {
        if (record == null || record.headers() == null) {
            return;
        }
        Header header = record.headers().lastHeader(headerKey);
        if (header == null || header.value() == null) {
            return;
        }
        String value = new String(header.value(), StandardCharsets.UTF_8);
        if (!value.isEmpty()) {
            MDC.put(mdcKey, value);
        }
    }
}
