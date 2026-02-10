package com.am.gateway.service;

import com.am.kafka.config.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Service to handle all Kafka publishing activities for the Gateway.
 * Encapsulates payload creation, logging, and sending to ensure consistency.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GatewayKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Trigger a portfolio calculation by sending a message to the calculation
     * topic.
     *
     * @param userId        The user ID to trigger calculation for.
     * @param portfolioId   The optional portfolio ID. If null/empty, triggers for
     *                      all portfolios.
     * @param correlationId The trace ID for request tracking.
     * @param source        The source of the trigger (e.g., "MANUAL", "AUTOMATED").
     */
    public void sendCalculationTrigger(String userId, String portfolioId, String correlationId, String source) {
        String payload;
        if (portfolioId != null && !portfolioId.isEmpty()) {
            payload = "{\"userId\": \"" + userId + "\", \"portfolioId\": \"" + portfolioId + "\"}";
        } else {
            payload = "{\"userId\": \"" + userId + "\"}";
        }

        // Log parameters before publishing with full payload as requested
        log.info("📤 Publishing [TRIGGER_CALCULATION] Source: {} | TraceID: {} | User: {} | Payload: {}",
                source, correlationId, userId, payload);

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    KafkaTopics.TRIGGER_CALCULATION,
                    correlationId,
                    payload);
            record.headers().add("X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8));

            kafkaTemplate.send(record);

            log.debug("✅ Successfully published trigger for TraceID: {}", correlationId);
        } catch (Exception e) {
            log.error("❌ Failed to publish trigger for User: {}, TraceID: {}", userId, correlationId, e);
        }
    }

    /**
     * Trigger a calculation with a generated correlation ID.
     */
    public void sendCalculationTrigger(String userId, String portfolioId, String source) {
        sendCalculationTrigger(userId, portfolioId, UUID.randomUUID().toString(), source);
    }
}
