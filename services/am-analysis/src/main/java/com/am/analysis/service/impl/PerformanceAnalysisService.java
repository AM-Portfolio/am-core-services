package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceAnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisCalculationService calculationService;
    private final AnalysisAccessValidator accessValidator;

    public PerformanceResponse getPerformance(String id, AnalysisEntityType type, String timeFrame, String userId) {
        // Dashboard calls pass id=null — aggregate across all portfolios for this user
        if (type == AnalysisEntityType.PORTFOLIO && (id == null || "ALL".equalsIgnoreCase(id) || "GLOBAL".equalsIgnoreCase(id))) {
            return getAggregatedPortfolioPerformance(userId, timeFrame);
        }

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

    /**
     * Aggregates performance across ALL portfolios owned by the user.
     * Used by dashboard (id=null) so we never accidentally access another user's entity.
     */
    private PerformanceResponse getAggregatedPortfolioPerformance(String userId, String timeFrame) {
        List<AnalysisEntity> portfolios = repository.findByOwnerIdAndType(userId, AnalysisEntityType.PORTFOLIO)
                .stream()
                .filter(e -> !e.getId().endsWith("_GLOBAL")) // avoid double-counting pre-aggregated globals
                .collect(Collectors.toList());

        if (portfolios.isEmpty()) {
            log.warn("No portfolio entities found for userId={} when computing dashboard performance", userId);
            return PerformanceResponse.builder()
                    .portfolioId("ALL")
                    .timeFrame(timeFrame)
                    .chartData(List.of())
                    .build();
        }

        // Build a virtual aggregated entity from all portfolios owned by this user
        double totalValue = portfolios.stream()
                .filter(p -> p.getPerformance() != null && p.getPerformance().getTotalValue() != null)
                .mapToDouble(p -> p.getPerformance().getTotalValue())
                .sum();

        double totalInvestment = portfolios.stream()
                .filter(p -> p.getPerformance() != null && p.getPerformance().getTotalInvestment() != null)
                .mapToDouble(p -> p.getPerformance().getTotalInvestment())
                .sum();

        double totalGainLoss = totalValue - totalInvestment;
        double totalGainLossPct = totalInvestment > 0 ? (totalGainLoss / totalInvestment) * 100.0 : 0.0;

        AnalysisEntity virtualEntity = AnalysisEntity.builder()
                .id("VIRTUAL_ALL_" + userId)
                .sourceId("ALL")
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(userId)
                .holdings(portfolios.stream()
                        .filter(p -> p.getHoldings() != null)
                        .flatMap(p -> p.getHoldings().stream())
                        .collect(Collectors.toList()))
                .performance(PerformanceSummary.builder()
                        .totalValue(totalValue)
                        .totalInvestment(totalInvestment)
                        .totalGainLoss(totalGainLoss)
                        .totalGainLossPercentage(totalGainLossPct)
                        .build())
                .build();

        log.info("Aggregated performance for userId={}: portfolios={}, totalValue={}, timeFrame={}",
                userId, portfolios.size(), totalValue, timeFrame);
        return calculationService.calculatePerformance(virtualEntity, timeFrame);
    }
}
