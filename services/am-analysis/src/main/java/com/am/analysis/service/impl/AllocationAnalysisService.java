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
    private final com.am.analysis.integration.MarketDataClient marketDataClient;
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
                .map(h -> h.getSymbol())
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .toList();

        Map<String, com.am.analysis.dto.MarketDataResponse.Match> marketData = marketDataClient.getSecurities(symbols);

        entity.getHoldings().forEach(h -> {
            if (marketData.containsKey(h.getSymbol())) {
                var match = marketData.get(h.getSymbol());
                if (h.getSector() == null || h.getSector().isEmpty()) {
                    h.setSector(match.getSector());
                }
                if (h.getIndustry() == null || h.getIndustry().isEmpty()) {
                    h.setIndustry(match.getIndustry());
                }
                if (h.getMarketCapType() == null || h.getMarketCapType().isEmpty()) {
                    h.setMarketCapType(match.getMarketCapType());
                }
            }
        });
    }
}
