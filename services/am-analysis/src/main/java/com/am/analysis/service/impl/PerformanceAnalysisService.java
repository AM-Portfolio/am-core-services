package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.service.aggregator.AnalysisAggregator;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.BootstrapTrigger;
import com.am.analysis.service.load.EntityLoadRequest;
import com.am.analysis.service.load.EntityLoadResult;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceAnalysisService {

    private final AnalysisRepository repository;
    private final AnalysisEntityLoadService entityLoadService;
    private final AnalysisCalculationService calculationService;
    private final AnalysisAccessValidator accessValidator;
    private final AnalysisAggregator aggregator;

    public PerformanceResponse getPerformance(String id, AnalysisEntityType type, String timeFrame, String userId) {
        if (type == AnalysisEntityType.PORTFOLIO && (id == null || "ALL".equalsIgnoreCase(id) || "GLOBAL".equalsIgnoreCase(id))) {
            return getAggregatedPortfolioPerformance(userId, timeFrame);
        }

        if (type == AnalysisEntityType.PORTFOLIO) {
            EntityLoadResult result = entityLoadService.loadOne(
                    EntityLoadRequest.onePortfolio(id, userId, BootstrapTrigger.HTTP_READ));
            if (result.empty()) {
                return emptyPerformance(id, timeFrame);
            }
            AnalysisEntity entity = result.entities().get(0);
            aggregator.applyLiveOverlay(List.of(entity));
            return alignChartWithLiveTotal(
                    calculationService.calculatePerformance(entity, timeFrame), entity);
        }

        String compositeId = type.name() + "_" + id;
        Optional<AnalysisEntity> entityOpt = repository.findById(compositeId);

        if (entityOpt.isPresent()) {
            accessValidator.verifyAccess(entityOpt.get(), userId);
            log.debug("Entity found for Performance: ID={}, Type={}, TimeFrame={}, User={}", id, type, timeFrame, userId);
            AnalysisEntity entity = entityOpt.get();
            aggregator.applyLiveOverlay(List.of(entity));
            return alignChartWithLiveTotal(
                    calculationService.calculatePerformance(entity, timeFrame), entity);
        }

        log.warn("Entity not found for Performance: ID={}, Type={}, User={}", id, type, userId);
        return emptyPerformance(id, timeFrame);
    }

    private PerformanceResponse getAggregatedPortfolioPerformance(String userId, String timeFrame) {
        EntityLoadResult result = entityLoadService.loadPortfoliosForUser(userId, BootstrapTrigger.HTTP_READ);
        List<AnalysisEntity> portfolios = result.entities();

        if (portfolios.isEmpty()) {
            return emptyPerformance("ALL", timeFrame);
        }

        aggregator.applyLiveOverlay(portfolios);

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
        return alignChartWithLiveTotal(
                calculationService.calculatePerformance(virtualEntity, timeFrame), virtualEntity);
    }

    /**
     * Aligns chart headline with live overlay total so performance widget matches summary.
     */
    private PerformanceResponse alignChartWithLiveTotal(PerformanceResponse response, AnalysisEntity entity) {
        if (response == null || entity.getPerformance() == null || entity.getPerformance().getTotalValue() == null) {
            return response;
        }
        double liveTotal = entity.getPerformance().getTotalValue();
        BigDecimal liveValue = BigDecimal.valueOf(liveTotal).setScale(2, RoundingMode.HALF_UP);

        List<PerformanceResponse.DataPoint> chartData = response.getChartData();
        if (chartData == null || chartData.isEmpty()) {
            chartData = List.of(PerformanceResponse.DataPoint.builder()
                    .date(java.time.LocalDate.now())
                    .value(liveValue)
                    .build());
        } else {
            List<PerformanceResponse.DataPoint> updated = new ArrayList<>(chartData);
            PerformanceResponse.DataPoint last = updated.get(updated.size() - 1);
            updated.set(updated.size() - 1, PerformanceResponse.DataPoint.builder()
                    .date(last.getDate())
                    .value(liveValue)
                    .build());
            chartData = updated;
        }

        return PerformanceResponse.builder()
                .portfolioId(response.getPortfolioId())
                .timeFrame(response.getTimeFrame())
                .totalReturnPercentage(response.getTotalReturnPercentage())
                .totalReturnValue(response.getTotalReturnValue())
                .chartData(chartData)
                .errorMessage(response.getErrorMessage())
                .build();
    }

    private PerformanceResponse emptyPerformance(String id, String timeFrame) {
        return PerformanceResponse.builder()
                .portfolioId(id)
                .timeFrame(timeFrame)
                .chartData(List.of())
                .build();
    }
}
