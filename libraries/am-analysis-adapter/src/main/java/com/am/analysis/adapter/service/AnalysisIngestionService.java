package com.am.analysis.adapter.service;

import com.am.analysis.adapter.event.AnalysisEntityIngestedEvent;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.repository.AnalysisRepository;
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
}
