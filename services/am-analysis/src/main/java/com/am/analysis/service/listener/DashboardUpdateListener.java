package com.am.analysis.service.listener;

import com.am.analysis.service.DashboardAnalysisService;
import com.am.kafka.config.KafkaTopics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to portfolio updates and triggers a dashboard refresh event.
 * This ensures the UI receives real-time updates on the /topic/dashboard/{userId} WebSocket.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DashboardUpdateListener {

    private final DashboardAnalysisService dashboardService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.PORTFOLIO_UPDATE, groupId = "am-analysis-dashboard-updater")
    public void onPortfolioUpdate(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("userId")) {
                String userId = node.get("userId").asText();
                log.info("[DashboardUpdateListener] Portfolio update received for user: {}. Triggering dashboard refresh.", userId);
                
                // This will calculate the latest summary and push it to the 'dashboard-update' Kafka topic
                dashboardService.publishDashboardUpdate(userId);
            }
        } catch (Exception e) {
            log.error("[DashboardUpdateListener] Failed to process portfolio update for dashboard refresh", e);
        }
    }
}
