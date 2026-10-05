package com.am.analysis.adapter.consumer;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.service.AnalysisIngestionService;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "am.analysis.adapter.portfolio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PortfolioEventListener {

    private final AnalysisEventMapper mapper;
    private final AnalysisIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.PORTFOLIO_UPDATE, groupId = "${spring.kafka.consumer.group-id:am-analysis-group-v2}")
    public void listen(String message) {
        log.info("Received Portfolio Update Event: {}", message);
        try {
            // 2. Deserialization
            PortfolioUpdateEvent event = objectMapper.readValue(message,
                    PortfolioUpdateEvent.class);

            if ("DELETE".equalsIgnoreCase(event.getAction())) {
                log.info("Deleting analysis data for portfolioId={}", event.getPortfolioId());
                ingestionService.delete(event.getPortfolioId(), event.getUserId());
            } else {
                var entity = mapper.mapPortfolioEvent(event);
                ingestionService.ingest(entity);
            }
        } catch (Exception e) {
            log.error("Failed to process portfolio event", e);
        }
    }
}
