package com.am.analysis.service.bootstrap;

import com.am.kafka.config.KafkaTopics;
import com.am.kafka.schema.TriggerCalcEvent;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.trace.TracingHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TriggerCalculationPublisher {

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
                kafkaTemplate.send(KafkaTopics.TRIGGER_CALCULATION, key, payload);

                flowLogger.complete(span,
                        "payload_bytes", payload.length(),
                        "trace_id_used", traceId);
            } catch (JsonProcessingException e) {
                flowLogger.fail(span, e);
            }
        }
    }
}
