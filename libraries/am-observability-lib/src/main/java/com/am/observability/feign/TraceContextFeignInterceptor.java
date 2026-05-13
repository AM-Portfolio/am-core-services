package com.am.observability.feign;

import com.am.observability.kafka.KafkaTraceHeaders;
import com.am.observability.mdc.MdcKeys;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;

/**
 * Feign request interceptor that propagates legacy correlation +
 * user identifiers as outbound headers. Micrometer Tracing already injects
 * the W3C {@code traceparent} header on Feign calls; this adds the human-
 * readable {@code X-Correlation-Id} and {@code X-User-Id} that downstream
 * services use for log enrichment.
 */
public class TraceContextFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        addIfAbsent(template, KafkaTraceHeaders.CORRELATION_ID, MDC.get(MdcKeys.CORRELATION_ID));
        addIfAbsent(template, KafkaTraceHeaders.USER_ID, MDC.get(MdcKeys.USER_ID));
    }

    private static void addIfAbsent(RequestTemplate template, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (template.headers() == null || !template.headers().containsKey(name)) {
            template.header(name, value);
        }
    }
}
