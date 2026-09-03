package com.am.analysis.service.load;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.service.bootstrap.PortfolioBootstrapTrigger;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import com.am.kafka.config.AnalysisEntityKeys;
import com.am.observability.flow.FlowLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Single entry point for loading PORTFOLIO {@link AnalysisEntity} records from Mongo.
 * On empty results, optionally fires debounced {@code am-trigger-calculation} bootstrap.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisEntityLoadService {

    private static final String BOOTSTRAP_SOURCE_HTTP = "BOOTSTRAP_HTTP_READ";
    private static final String BOOTSTRAP_SOURCE_WS = "BOOTSTRAP_ENTITY_MISSING";

    private final AnalysisRepository repository;
    private final AnalysisAccessValidator accessValidator;
    private final PortfolioBootstrapTrigger portfolioBootstrapTrigger;
    private final FlowLogger flowLogger;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.demo.portfolio-id:f969745c-f492-4b86-88ed-6588e9f28bb3}")
    private String demoPortfolioId;

    @Value("${app.demo.owner-id:admin}")
    private String demoOwnerId;

    public EntityLoadResult loadAll(EntityLoadRequest request) {
        return loadPortfoliosForUser(request.userId(), request.triggerSource());
    }

    public EntityLoadResult loadOne(EntityLoadRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return EntityLoadResult.empty(false);
        }
        String portfolioId = request.sourceId();
        if (portfolioId == null || portfolioId.isBlank()) {
            return loadAll(request);
        }

        String entityId = AnalysisEntityKeys.portfolioEntityId(portfolioId, request.userId());
        Optional<AnalysisEntity> entityOpt = repository.findById(entityId);

        if (entityOpt.isPresent()) {
            AnalysisEntity entity = entityOpt.get();
            accessValidator.verifyAccess(entity, request.userId());
            flowLogger.step("analysis.entity_load.found",
                    "entityId", entityId,
                    "userId", request.userId(),
                    "scope", "ONE");
            return EntityLoadResult.of(List.of(entity), false);
        }

        log.warn("[EntityLoad] Portfolio entity not found: entityId={}, userId={}", entityId, request.userId());
        boolean bootstrapRequested = fireBootstrap(request.userId(), portfolioId, request.triggerSource());
        return EntityLoadResult.empty(bootstrapRequested);
    }

    public EntityLoadResult loadPortfoliosForUser(String userId, BootstrapTrigger trigger) {
        if (userId == null || userId.isBlank()) {
            return EntityLoadResult.empty(false);
        }

        List<AnalysisEntity> portfolios = repository.findByOwnerIdAndType(userId, AnalysisEntityType.PORTFOLIO)
                .stream()
                .filter(p -> p.getSourceId() == null || !AnalysisEntityKeys.isGlobalSourceId(p.getSourceId()))
                .collect(Collectors.toList());

        if (portfolios.isEmpty()) {
            // Demo portfolio injection logic
            if (demoPortfolioId != null && !demoPortfolioId.isBlank() && !hasDismissedDemo(userId)) {
                String demoEntityId = AnalysisEntityKeys.portfolioEntityId(demoPortfolioId, demoOwnerId);
                Optional<AnalysisEntity> demoOpt = repository.findById(demoEntityId);
                if (demoOpt.isPresent()) {
                    portfolios = new ArrayList<>();
                    portfolios.add(demoOpt.get());
                    log.info("[EntityLoad] Injected demo portfolio {} for user {}", demoPortfolioId, userId);
                }
            }
        }

        if (!portfolios.isEmpty()) {
            flowLogger.step("analysis.entity_load.found",
                    "userId", userId,
                    "scope", "ALL",
                    "count", portfolios.size());
            return EntityLoadResult.of(portfolios, false);
        }

        log.warn("[EntityLoad] No portfolio analysis entities for userId={}", userId);
        boolean bootstrapRequested = fireBootstrap(userId, null, trigger);
        return EntityLoadResult.empty(bootstrapRequested);
    }

    private boolean hasDismissedDemo(String userId) {
        if (redisTemplate == null) return false;
        try {
            String val = redisTemplate.opsForValue().get("demo:dismissed:" + userId);
            return "true".equalsIgnoreCase(val);
        } catch (Exception e) {
            log.warn("[EntityLoad] Failed to check demo dismissed status in Redis for {}: {}", userId, e.getMessage());
            return false;
        }
    }

    public Optional<AnalysisEntity> loadGlobalPortfolio(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(AnalysisEntityKeys.globalEntityId(userId))
                .filter(entity -> userId.equals(entity.getOwnerId()));
    }

    private boolean fireBootstrap(String userId, String portfolioId, BootstrapTrigger trigger) {
        String source = trigger == BootstrapTrigger.WS_SUBSCRIBE
                ? BOOTSTRAP_SOURCE_WS
                : BOOTSTRAP_SOURCE_HTTP;
        return portfolioBootstrapTrigger.requestBootstrap(userId, portfolioId, source, null);
    }
}
