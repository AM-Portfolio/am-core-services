package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceAnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisCalculationService calculationService;
    private final AnalysisAccessValidator accessValidator;

    public PerformanceResponse getPerformance(String id, AnalysisEntityType type, String timeFrame, String userId) {
        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            accessValidator.verifyAccess(entityOpt.get(), userId);
            log.debug("Entity found for Performance: ID={}, Type={}, TimeFrame={}, User={}", id, type, timeFrame, userId);
            return calculationService.calculatePerformance(entityOpt.get(), timeFrame);
        }

        log.warn("Entity not found for Performance: ID={}, Type={}, User={}", id, type, userId);
        return PerformanceResponse.builder()
                .portfolioId(id)
                .timeFrame(timeFrame)
                .chartData(List.of())
                .build();
    }
}
