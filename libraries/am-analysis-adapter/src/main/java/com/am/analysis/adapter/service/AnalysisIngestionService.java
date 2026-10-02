package com.am.analysis.adapter.service;

import com.am.analysis.adapter.event.AnalysisEntityIngestedEvent;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.kafka.config.AnalysisEntityKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "am.analysis.adapter.ingestion", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AnalysisIngestionService {
    private final AnalysisRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /** Injected lazily to avoid a circular dep — dashboard service lives in am-analysis,
     *  not in this adapter library. Null-safe: if not present (e.g. tests), skip refresh. */
    @Autowired(required = false)
    private DashboardRefreshPort dashboardRefreshPort;

    public void ingest(AnalysisEntity entity) {
        log.info("Ingesting analysis data for {} (Type: {})", entity.getSourceId(), entity.getType());
        repository.save(entity);
        log.info("[AnalysisIngestionService] Successfully persisted {} to MongoDB", entity.getSourceId());
        eventPublisher.publishEvent(new AnalysisEntityIngestedEvent(this, entity));
    }

    public void delete(String portfolioId, String userId) {
        String effectivePortfolioId = portfolioId != null && !portfolioId.isBlank()
                                ? portfolioId
                                : AnalysisEntityKeys.GLOBAL_SOURCE_ID;
        String entityId;
        if (AnalysisEntityKeys.isGlobalSourceId(effectivePortfolioId)) {
            entityId = AnalysisEntityKeys.globalEntityId(userId);
        } else {
            entityId = AnalysisEntityKeys.portfolioEntityId(effectivePortfolioId, userId);
        }

        log.info("[AnalysisIngestionService] Attempting to delete entity id={} for portfolioId={} userId={}",
                entityId, effectivePortfolioId, userId);

        repository.findById(entityId).ifPresentOrElse(
            entity -> {
                log.info("Deleting analysis data for {} (Type: {})", entity.getSourceId(), entity.getType());
                repository.delete(entity);
                log.info("[AnalysisIngestionService] Successfully deleted {} from MongoDB", entity.getSourceId());
                // Trigger dashboard refresh AFTER Mongo deletion is committed so the
                // DashboardUpdateListener (which skips DELETE events) doesn't race us.
                if (dashboardRefreshPort != null) {
                    dashboardRefreshPort.publishDashboardUpdate(userId);
                }
            },
            () -> log.warn("[AnalysisIngestionService] No entity found for id={} — already deleted or never ingested (portfolioId={}, userId={})",
                    entityId, effectivePortfolioId, userId)
        );
    }
}
