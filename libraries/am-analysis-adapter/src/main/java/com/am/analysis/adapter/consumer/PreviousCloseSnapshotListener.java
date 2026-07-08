package com.am.analysis.adapter.consumer;

import com.am.kafka.config.KafkaTopics;
import com.am.kafka.schema.PreviousCloseSnapshot;
import com.am.kafka.service.PreviousCloseRedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "am.analysis.adapter.prev-close", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PreviousCloseSnapshotListener {

    private final PreviousCloseRedisService previousCloseRedisService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.PREVIOUS_CLOSE_SNAPSHOT, groupId = "am-analysis-group")
    public void listen(String message) {
        log.info("Received Previous Close Snapshot: {}", message);
        try {
            PreviousCloseSnapshot snapshot = objectMapper.readValue(message, PreviousCloseSnapshot.class);
            previousCloseRedisService.write(snapshot.getId(), snapshot.getPreviousCloseValues());
        } catch (Exception e) {
            log.error("Failed to process previous close snapshot: {}", message, e);
        }
    }
}
