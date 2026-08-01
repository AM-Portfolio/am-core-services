package com.am.analysis.service.bootstrap;

import com.am.kafka.config.KafkaTopics;
import com.am.kafka.schema.TriggerCalcEvent;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.trace.TracingHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriggerCalculationPublisher {

    private static final long PUBLISH_TIMEOUT_SECONDS = 2L;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;
    private final TracingHelper tracingHelper;

    public void publish(String userId, String portfolioId, String source, String inheritedTraceId) {
        String debounceKey = portfolioId != null ? portfolioId : "global";

        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.trigger_calculation",
                "portfolioId", debounceKey,
                "userId", userId,
                "source", source,
                "topic", KafkaTopics.TRIGGER_CALCULATION)) {
            try {
                String traceId = inheritedTraceId != null && !inheritedTraceId.isEmpty()
                        ? inheritedTraceId
                        : tracingHelper.currentTraceIdOrNew();
                String spanId = tracingHelper.currentSpanIdOrNew();

                TriggerCalcEvent event = TriggerCalcEvent.builder()
                        .traceId(traceId)
                        .spanId(spanId)
                        .userId(userId)
                        .portfolioId(portfolioId)
                        .triggerSource(source)
                        .timestamp(Instant.now())
                        .build();

                String payload = objectMapper.writeValueAsString(event);
                String key = portfolioId != null ? portfolioId : (userId != null ? userId : "global");
                // Never block HTTP read path for ~60s when Kafka metadata is unavailable.
                kafkaTemplate.send(KafkaTopics.TRIGGER_CALCULATION, key, payload)
                        .get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                flowLogger.complete(span,
                        "payload_bytes", payload.length(),
                        "trace_id_used", traceId);
            } catch (TimeoutException e) {
                log.warn("[Bootstrap] Kafka trigger publish timed out ({}s) for portfolioId={} — returning without blocking HTTP",
                        PUBLISH_TIMEOUT_SECONDS, debounceKey);
                flowLogger.fail(span, e);
            } catch (JsonProcessingException e) {
                flowLogger.fail(span, e);
            } catch (Exception e) {
                log.warn("[Bootstrap] Kafka trigger publish failed for portfolioId={}: {}",
                        debounceKey, e.getMessage());
                flowLogger.fail(span, e);
            }
        }
    }
}
