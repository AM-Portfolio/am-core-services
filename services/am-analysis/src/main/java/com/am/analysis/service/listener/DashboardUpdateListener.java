package com.am.analysis.service.listener;

import com.am.analysis.service.DashboardAnalysisService;
import com.am.kafka.config.KafkaTopics;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to portfolio updates and triggers a dashboard refresh event.
 * The fan-out to /topic/dashboard/{userId} is performed downstream by the
 * gateway's KafkaRelayService once the resulting dashboard-update event is
 * published.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DashboardUpdateListener {

    private final DashboardAnalysisService dashboardService;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;

    @KafkaListener(topics = KafkaTopics.PORTFOLIO_UPDATE, groupId = "am-analysis-dashboard-updater")
    public void onPortfolioUpdate(String message) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.consume.portfolio_update",
                "payload_bytes", message == null ? 0 : message.length())) {
            try {
                JsonNode node = objectMapper.readTree(message);
                if (!node.has("userId")) {
                    flowLogger.fail(span, null, "reason", "missing_userId");
                    return;
                }
                String userId = node.get("userId").asText();

                // Skip refresh for DELETE events — PortfolioEventListener (am-analysis-group-v2)
                // deletes Mongo then refreshes via DashboardRefreshPort. Refreshing here would
                // race that deletion and could serve stale data one final time.
                // Trim matches PortfolioEventListener so " delete " does not re-open that race.
                String action = node.has("action") && !node.get("action").isNull()
                        ? node.get("action").asText() : null;
                if (action != null && "DELETE".equalsIgnoreCase(action.trim())) {
                    flowLogger.complete(span, "userId", userId, "skipped", "DELETE");
                    return;
                }

                // Ack path must stay fast — heavy Redis/quote work on this thread previously
                // ejected the consumer group (max.poll.interval) and left am-portfolio-update lagging.
                final String refreshUserId = userId;
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        dashboardService.publishDashboardUpdate(refreshUserId);
                    } catch (Exception ex) {
                        log.warn("Async dashboard refresh failed userId={}: {}", refreshUserId, ex.getMessage());
                    }
                });
                flowLogger.complete(span, "userId", userId, "refresh", "async");
            } catch (Exception e) {
                flowLogger.fail(span, e);
            }
        }
    }
}
