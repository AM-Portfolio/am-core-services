package com.am.analysis.service;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.config.PortfolioStreamingProperties;
import com.am.kafka.config.AnalysisEntityKeys;
import com.am.kafka.config.InterestRegistryKeys;
import com.am.kafka.config.KafkaTopics;
import com.am.kafka.service.InterestRegistryService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Computes portfolio snapshots from Mongo {@link AnalysisEntity} and publishes
 * to {@link KafkaTopics#PORTFOLIO_STREAM} for gateway WebSocket relay.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioStreamingService {

    private final AnalysisRepository analysisRepository;
    private final AnalysisEventMapper analysisEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final FlowLogger flowLogger;
    private final InterestRegistryService interestRegistry;
    private final PortfolioStreamingProperties properties;

    /**
     * Load entity, optionally overlay live tick prices, map to event, publish Kafka.
     *
     * @return true if a stream message was published to Kafka
     */
    public boolean publishPortfolioStream(String userId, String portfolioId, Map<String, LivePriceTick> liveTicks) {
        if (!properties.isEnabled()) {
            flowLogger.step("analysis.portfolio_stream.disabled", "userId", userId);
            return false;
        }
        if (userId == null || userId.isBlank()) {
            return false;
        }

        String entityId = AnalysisEntityKeys.portfolioEntityId(portfolioId, userId);
        Optional<AnalysisEntity> entityOpt = analysisRepository.findById(entityId);
        if (entityOpt.isEmpty()) {
            flowLogger.step("analysis.portfolio_stream.entity_not_found",
                    "userId", userId,
                    "portfolioId", portfolioId != null ? portfolioId : "GLOBAL",
                    "entityId", entityId);
            return false;
        }

        AnalysisEntity entity = entityOpt.get();
        if (!userId.equals(entity.getOwnerId())) {
            flowLogger.step("analysis.portfolio_stream.owner_mismatch",
                    "userId", userId, "entityId", entityId);
            return false;
        }

        LivePriceOverlayHelper.apply(entity, liveTicks);
        entity.setLastUpdated(LocalDateTime.now());
        PortfolioUpdateEvent event = analysisEventMapper.mapEntityToPortfolioUpdateEvent(entity);
        if (event == null) {
            return false;
        }

        publishEvent(event, userId, portfolioId);
        return true;
    }

    /**
     * After adapter ingest: push stream only if this user is actively watching the matching portfolio channel.
     */
    public void publishIfUserWatching(AnalysisEntity entity) {
        if (!properties.isEnabled() || entity == null
                || entity.getType() != AnalysisEntityType.PORTFOLIO
                || entity.getOwnerId() == null) {
            return;
        }

        String userId = entity.getOwnerId();
        Optional<String> watchedOpt = interestRegistry.getWatchedPortfolio(userId);
        if (watchedOpt.isEmpty()) {
            return;
        }

        String watched = watchedOpt.get();
        if (InterestRegistryKeys.isDashboardChannel(watched)) {
            return;
        }

        if (!matchesWatchTarget(watched, entity.getSourceId())) {
            return;
        }

        publishPortfolioStream(userId, watched, Map.of());
    }

    static boolean matchesWatchTarget(String watchedPortfolioId, String entitySourceId) {
        if (watchedPortfolioId == null || watchedPortfolioId.isBlank()) {
            return AnalysisEntityKeys.isGlobalSourceId(entitySourceId);
        }
        if (AnalysisEntityKeys.isGlobalSourceId(watchedPortfolioId)
                || "ALL".equalsIgnoreCase(watchedPortfolioId)) {
            return AnalysisEntityKeys.isGlobalSourceId(entitySourceId);
        }
        return watchedPortfolioId.equals(entitySourceId);
    }

    private void publishEvent(PortfolioUpdateEvent event, String userId, String portfolioId) {
        try (FlowSpan span = flowLogger.start("analysis.kafka.publish.portfolio_stream",
                "userId", userId,
                "portfolioId", portfolioId != null ? portfolioId : "GLOBAL",
                "topic", KafkaTopics.PORTFOLIO_STREAM)) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(KafkaTopics.PORTFOLIO_STREAM, userId, payload);
                int holdings = event.getEquities() != null ? event.getEquities().size() : 0;
                flowLogger.complete(span, "payload_bytes", payload.length(), "holdings", holdings);
            } catch (JsonProcessingException e) {
                flowLogger.fail(span, e);
            }
        }
    }

}
