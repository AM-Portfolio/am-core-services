package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.service.LivePriceOverlayHelper;
import com.am.analysis.service.LivePriceTick;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.BootstrapTrigger;
import com.am.analysis.service.load.EntityLoadRequest;
import com.am.analysis.service.load.EntityLoadResult;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationAnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisEntityLoadService entityLoadService;
    private final AnalysisCalculationService calculationService;
    private final com.am.market.client.service.MarketDataClientService marketDataClientService;
    private final AnalysisAccessValidator accessValidator;

    public AllocationResponse getAllocation(String id, AnalysisEntityType type, String userId, AnalysisGroupBy groupBy) {
        return getAllocation(id, type, userId, groupBy, Map.of());
    }

    public AllocationResponse getAllocation(String id, AnalysisEntityType type, String userId, AnalysisGroupBy groupBy,
                                            Map<String, LivePriceTick> liveTicks) {
        if ("ALL".equals(id) && type == AnalysisEntityType.PORTFOLIO) {
            Optional<AnalysisEntity> globalOpt = entityLoadService.loadGlobalPortfolio(userId);
            if (globalOpt.isPresent()) {
                log.info("Found pre-calculated GLOBAL allocation for user: {}", userId);
                AnalysisEntity global = globalOpt.get();
                enrichWithMarketData(global);
                return calculationService.calculateAllocation(global, groupBy);
            }

            log.info("Performing dynamic aggregation for all portfolios for user: {}", userId);
            EntityLoadResult result = entityLoadService.loadPortfoliosForUser(userId, BootstrapTrigger.HTTP_READ);
            List<AnalysisEntity> allPortfolios = result.entities();

            if (liveTicks != null && !liveTicks.isEmpty()) {
                LivePriceOverlayHelper.applyAll(allPortfolios, liveTicks);
            }

            if (allPortfolios.isEmpty()) {
                return emptyAllocation(id);
            }

            AnalysisEntity globalEntity = AnalysisEntity.builder()
                    .id("VIRTUAL_ALL")
                    .type(type)
                    .ownerId(userId)
                    .holdings(allPortfolios.stream()
                            .filter(p -> p.getHoldings() != null)
                            .flatMap(p -> p.getHoldings().stream())
                            .toList())
                    .build();

            enrichWithMarketData(globalEntity);
            return calculationService.calculateAllocation(globalEntity, groupBy);
        }

        if (type == AnalysisEntityType.PORTFOLIO) {
            EntityLoadResult result = entityLoadService.loadOne(
                    EntityLoadRequest.onePortfolio(id, userId, BootstrapTrigger.HTTP_READ));
            if (result.empty()) {
                return emptyAllocation(id);
            }
            AnalysisEntity entity = result.entities().get(0);
            enrichWithMarketData(entity);
            return calculationService.calculateAllocation(entity, groupBy);
        }

        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            accessValidator.verifyAccess(entityOpt.get(), userId);
            AnalysisEntity entity = entityOpt.get();
            log.debug("Entity found for Allocation: ID={}, Type={}, User={}, GroupBy={}", id, type, userId, groupBy);
            enrichWithMarketData(entity);
            return calculationService.calculateAllocation(entity, groupBy);
        }

        log.warn("Entity not found for Analysis: ID={}, Type={}, User={}", id, type, userId);
        return emptyAllocation(id);
    }

    private AllocationResponse emptyAllocation(String id) {
        return AllocationResponse.builder()
                .portfolioId(id)
                .sectors(List.of())
                .assetClasses(List.of())
                .build();
    }

    private void enrichWithMarketData(AnalysisEntity entity) {
        if (entity.getHoldings() == null || entity.getHoldings().isEmpty()) {
            return;
        }

        List<String> symbolsToFetch = entity.getHoldings().stream()
                .filter(h -> h.getClassification() == null ||
                            h.getClassification().getSector() == null ||
                            h.getClassification().getSector().isEmpty())
                .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                .map(h -> h.getIdentity().getSymbol())
                .distinct()
                .toList();

        if (symbolsToFetch.isEmpty()) {
            log.debug("All holdings already have metadata, skipping enrichment for entity: {}", entity.getId());
            return;
        }

        log.info("Fetching missing market metadata for {} symbols in entity: {}", symbolsToFetch.size(), entity.getId());
        Map<String, com.am.portfolio.client.market.model.SecurityMetadata> marketData = marketDataClientService.searchSecurities(symbolsToFetch);

        entity.getHoldings().forEach(h -> {
            if (h.getIdentity() != null && marketData.containsKey(h.getIdentity().getSymbol())) {
                var metadata = marketData.get(h.getIdentity().getSymbol());

                if (h.getClassification() == null) {
                    h.setClassification(new com.am.analysis.adapter.model.components.AssetClassification());
                }

                var cls = h.getClassification();
                if (cls.getSector() == null || cls.getSector().isEmpty()) {
                    cls.setSector(metadata.getSector());
                }
                if (cls.getIndustry() == null || cls.getIndustry().isEmpty()) {
                    cls.setIndustry(metadata.getIndustry());
                }
                if (cls.getMarketCapType() == null || cls.getMarketCapType().isEmpty()) {
                    cls.setMarketCapType(metadata.getMarketCapType());
                }
            }
        });
    }
}
