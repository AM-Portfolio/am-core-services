package com.am.gateway.service;

import com.am.kafka.config.KafkaTopics;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.kafka.KafkaTraceHeaders;
import com.am.observability.trace.TracingHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Service to handle all Kafka publishing activities for the Gateway.
 * Encapsulates payload creation, logging, and sending to ensure consistency.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GatewayKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final FlowLogger flowLogger;
    private final TracingHelper tracingHelper;

    /**
     * Trigger a portfolio calculation by sending a message to the calculation
     * topic. Uses the current span's trace id as the correlation id so logs
     * stay searchable end-to-end.
     */
    public void sendCalculationTrigger(String userId, String portfolioId, String correlationId, String source) {
        String payload;
        if (portfolioId != null && !portfolioId.isEmpty()) {
            payload = "{\"userId\": \"" + userId + "\", \"portfolioId\": \"" + portfolioId + "\"}";
        } else {
            payload = "{\"userId\": \"" + userId + "\"}";
        }

        String effectiveCorrelation = (correlationId == null || correlationId.isEmpty())
                ? tracingHelper.currentTraceIdOrNew()
                : correlationId;

        try (FlowSpan span = flowLogger.start("gateway.kafka.publish.trigger_calculation",
                "userId", userId,
                "portfolioId", portfolioId != null ? portfolioId : "ALL",
                "source", source,
                "correlationId", effectiveCorrelation,
                "topic", KafkaTopics.TRIGGER_CALCULATION)) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        KafkaTopics.TRIGGER_CALCULATION,
                        effectiveCorrelation,
                        payload);
                record.headers().add(KafkaTraceHeaders.CORRELATION_ID,
                        effectiveCorrelation.getBytes(StandardCharsets.UTF_8));

                kafkaTemplate.send(record);
                flowLogger.complete(span, "payload_bytes", payload.length());
            } catch (Exception e) {
                flowLogger.fail(span, e);
            }
        }
    }

    /**
     * Trigger a calculation deriving the correlation id from the current trace.
     */
    public void sendCalculationTrigger(String userId, String portfolioId, String source) {
        sendCalculationTrigger(userId, portfolioId, tracingHelper.currentTraceIdOrNew(), source);
    }
}
