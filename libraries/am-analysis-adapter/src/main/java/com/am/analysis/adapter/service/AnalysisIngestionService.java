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

        // Refresh after Mongo is committed so DashboardUpdateListener's parallel (often early)
        // empty/demo push cannot be the last word for CREATE/UPDATE events.
        String ownerId = entity.getOwnerId();
        if (dashboardRefreshPort != null && ownerId != null && !ownerId.isBlank()) {
            dashboardRefreshPort.publishDashboardUpdate(ownerId);
        }
    }

    public void delete(String portfolioId, String userId) {
        // Never fall back to GLOBAL — a blank/malformed DELETE must not wipe the user's aggregate.
        if (portfolioId == null || portfolioId.isBlank()) {
            log.warn("[AnalysisIngestionService] Skipping delete — blank portfolioId userId={}", userId);
            return;
        }
        if (AnalysisEntityKeys.isGlobalSourceId(portfolioId)) {
            log.warn("[AnalysisIngestionService] Skipping delete — refusing GLOBAL/all portfolioId={} userId={}",
                    portfolioId, userId);
            return;
        }

        String entityId = AnalysisEntityKeys.portfolioEntityId(portfolioId, userId);
        log.info("[AnalysisIngestionService] Attempting to delete entity id={} for portfolioId={} userId={}",
                entityId, portfolioId, userId);

        repository.findById(entityId).ifPresentOrElse(
            entity -> {
                log.info("Deleting analysis data for {} (Type: {})", entity.getSourceId(), entity.getType());
                repository.delete(entity);
                log.info("[AnalysisIngestionService] Successfully deleted {} from MongoDB", entity.getSourceId());
            },
            () -> log.warn("[AnalysisIngestionService] No entity found for id={} — already deleted or never ingested (portfolioId={}, userId={})",
                    entityId, portfolioId, userId)
        );

        // Always refresh after a DELETE attempt. DashboardUpdateListener skips DELETE to avoid racing
        // the Mongo delete; if the row was already gone, UI/cache can still be stale without this.
        if (dashboardRefreshPort != null && userId != null && !userId.isBlank()) {
            dashboardRefreshPort.publishDashboardUpdate(userId);
        }
    }
}
