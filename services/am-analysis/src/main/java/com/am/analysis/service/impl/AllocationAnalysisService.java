package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.AllocationResponse;
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
    private final AnalysisCalculationService calculationService;
    private final com.am.market.client.service.MarketDataClientService marketDataClientService;
    private final AnalysisAccessValidator accessValidator;

    public AllocationResponse getAllocation(String id, AnalysisEntityType type, String userId) {
        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            accessValidator.verifyAccess(entityOpt.get(), userId);
            AnalysisEntity entity = entityOpt.get();
            log.debug("Entity found for Allocation: ID={}, Type={}, User={}", id, type, userId);
            enrichWithMarketData(entity);
            return calculationService.calculateAllocation(entity);
        }
        
        log.warn("Entity not found for Analysis: ID={}, Type={}, User={}", id, type, userId);
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

        List<String> symbols = entity.getHoldings().stream()
                .filter(h -> h.getIdentity() != null && h.getIdentity().getSymbol() != null)
                .map(h -> h.getIdentity().getSymbol())
                .distinct()
                .toList();

        Map<String, com.am.portfolio.client.market.model.SecurityMetadata> marketData = marketDataClientService.searchSecurities(symbols);

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
