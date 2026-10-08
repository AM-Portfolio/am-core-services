package com.am.analysis.adapter.service;

import com.am.analysis.adapter.event.AnalysisEntityIngestedEvent;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.kafka.config.AnalysisEntityKeys;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public void ingest(AnalysisEntity entity) {
        log.info("Ingesting analysis data for {} (Type: {})", entity.getSourceId(), entity.getType());
        repository.save(entity);
        log.info("[AnalysisIngestionService] Successfully persisted {} to MongoDB", entity.getSourceId());
        eventPublisher.publishEvent(new AnalysisEntityIngestedEvent(this, entity));
    }

    /**
     * Removes the analysis portfolio entity keyed by portfolioId so DELETE
     * fan-out from am-portfolio does not leave empty ghost rows.
     *
     * Never falls back to GLOBAL: a blank/malformed DELETE must not wipe the
     * user's aggregate portfolio entity.
     */
    public void deletePortfolio(PortfolioUpdateEvent event) {
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            log.warn("Skipping analysis DELETE — missing userId");
            return;
        }
        String portfolioId = event.getPortfolioId();
        if (portfolioId == null || portfolioId.isBlank()) {
            log.warn("Skipping analysis DELETE — blank portfolioId userId={}", event.getUserId());
            return;
        }
        if (AnalysisEntityKeys.isGlobalSourceId(portfolioId)) {
            log.warn("Skipping analysis DELETE — refusing GLOBAL/all portfolioId={} userId={}",
                    portfolioId, event.getUserId());
            return;
        }

        String entityId = AnalysisEntityKeys.portfolioEntityId(portfolioId, event.getUserId());
        if (repository.existsById(entityId)) {
            repository.deleteById(entityId);
            log.info("Deleted analysis portfolio entity id={} sourceId={} owner={}",
                    entityId, portfolioId, event.getUserId());
        } else {
            log.info("Analysis DELETE no-op — entity not found id={} owner={}",
                    entityId, event.getUserId());
        }
    }
}
