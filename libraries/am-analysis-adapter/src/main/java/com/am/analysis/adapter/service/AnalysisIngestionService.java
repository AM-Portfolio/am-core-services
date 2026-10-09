package com.am.analysis.adapter.service;

import com.am.analysis.adapter.event.AnalysisEntityIngestedEvent;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.kafka.config.AnalysisEntityKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
        // Same broker/name re-upload must overwrite one logical book (Trade remaps by name).
        collapseSupersededPortfolios(entity);
        repository.save(entity);
        log.info("[AnalysisIngestionService] Successfully persisted {} to MongoDB", entity.getSourceId());
        eventPublisher.publishEvent(new AnalysisEntityIngestedEvent(this, entity));

        // Async: must not block the Kafka listener thread. Sync refresh (Redis/market quotes)
        // previously exceeded max.poll.interval and ejected am-analysis-group-v2 from the cluster.
        refreshDashboardAsync(entity.getOwnerId());
    }

    /**
     * Deletes other PORTFOLIO rows for the same owner that share logical identity
     * (canonical broker, else display name). Remaps the incoming entity onto the
     * oldest surviving same-identity id when present so watchers keep a stable key.
     */
    private void collapseSupersededPortfolios(AnalysisEntity incoming) {
        if (incoming == null || incoming.getType() != AnalysisEntityType.PORTFOLIO) {
            return;
        }
        String ownerId = incoming.getOwnerId();
        if (ownerId == null || ownerId.isBlank()) {
            return;
        }
        if (AnalysisEntityKeys.isGlobalSourceId(incoming.getSourceId())) {
            return;
        }

        String identity = PortfolioIdentity.keyOf(incoming);
        String incomingFingerprint = PortfolioIdentity.holdingsFingerprint(incoming);

        List<AnalysisEntity> owned = repository.findByOwnerIdAndType(ownerId, AnalysisEntityType.PORTFOLIO);
        if (owned == null || owned.isEmpty()) {
            return;
        }

        AnalysisEntity keep = null;
        for (AnalysisEntity existing : owned) {
            if (existing == null || Objects.equals(existing.getId(), incoming.getId())) {
                continue;
            }
            if (AnalysisEntityKeys.isGlobalSourceId(existing.getSourceId())) {
                continue;
            }
            if (!sameLogicalPortfolio(identity, incomingFingerprint, existing)) {
                continue;
            }
            if (keep == null) {
                keep = existing;
            } else {
                // Prefer the earliest-seen stable id; drop the rest.
                log.info("[AnalysisIngestionService] Removing duplicate portfolio entity id={} sourceId={} identity={}",
                        existing.getId(), existing.getSourceId(), identity);
                repository.delete(existing);
            }
        }

        if (keep != null) {
            log.info("[AnalysisIngestionService] Collapsing portfolio identity={} — remapping {} → keep id={} sourceId={}",
                    identity, incoming.getSourceId(), keep.getId(), keep.getSourceId());
            // Drop the keep row; incoming will be saved under keep's ids with latest holdings.
            repository.delete(keep);
            incoming.setId(keep.getId());
            incoming.setSourceId(keep.getSourceId());
            if (incoming.getPortfolioName() == null || incoming.getPortfolioName().isBlank()) {
                incoming.setPortfolioName(keep.getPortfolioName());
            }
            if (incoming.getBrokerType() == null || incoming.getBrokerType().isBlank()) {
                incoming.setBrokerType(keep.getBrokerType());
            }
        }
    }

    private static boolean sameLogicalPortfolio(String incomingIdentity, String incomingFingerprint,
                                                AnalysisEntity existing) {
        String existingIdentity = PortfolioIdentity.keyOf(existing);
        if (incomingIdentity != null && incomingIdentity.equals(existingIdentity)) {
            return true;
        }
        // Legacy rows (no name/broker) that look like the same book as this upload.
        if (incomingFingerprint != null && existingIdentity == null) {
            return incomingFingerprint.equals(PortfolioIdentity.holdingsFingerprint(existing));
        }
        return false;
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
        // Async — same max.poll.interval hazard as ingest.
        refreshDashboardAsync(userId);
    }

    private void refreshDashboardAsync(String userId) {
        if (dashboardRefreshPort == null || userId == null || userId.isBlank()) {
            return;
        }
        DashboardRefreshPort port = dashboardRefreshPort;
        CompletableFuture.runAsync(() -> {
            try {
                port.publishDashboardUpdate(userId);
            } catch (Exception e) {
                log.warn("[AnalysisIngestionService] Async dashboard refresh failed userId={}: {}",
                        userId, e.getMessage());
            }
        });
    }
}
