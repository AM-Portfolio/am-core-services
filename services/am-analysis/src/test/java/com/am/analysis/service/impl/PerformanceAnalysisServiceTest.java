package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.analysis.service.load.BootstrapTrigger;
import com.am.analysis.service.load.EntityLoadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Verifies aggregated dashboard performance is computed per user (virtual entity carries ownerId).
 */
@ExtendWith(MockitoExtension.class)
class PerformanceAnalysisServiceTest {

    @Mock
    private com.am.analysis.adapter.repository.AnalysisRepository repository;

    @Mock
    private AnalysisEntityLoadService entityLoadService;

    @Mock
    private AnalysisCalculationService calculationService;

    @Mock
    private com.am.analysis.service.validator.AnalysisAccessValidator accessValidator;

    @InjectMocks
    private PerformanceAnalysisService performanceAnalysisService;

  @BeforeEach
    void setUp() {
        when(calculationService.calculatePerformance(any(AnalysisEntity.class), eq("1D")))
                .thenAnswer(invocation -> {
                    AnalysisEntity entity = invocation.getArgument(0);
                    return PerformanceResponse.builder()
                            .portfolioId(entity.getSourceId())
                            .timeFrame("1D")
                            .totalReturnPercentage(entity.getPerformance() != null
                                    ? entity.getPerformance().getTotalValue() : 0.0)
                            .build();
                });
    }

    @Test
    void aggregatedPerformance_buildsVirtualEntityWithOwnerIdPerUser() {
        when(entityLoadService.loadPortfoliosForUser(eq("user-a"), eq(BootstrapTrigger.HTTP_READ)))
                .thenReturn(entityLoadResult("user-a", 500000.0));
        when(entityLoadService.loadPortfoliosForUser(eq("user-b"), eq(BootstrapTrigger.HTTP_READ)))
                .thenReturn(entityLoadResult("user-b", 50000.0));

        PerformanceResponse userA = performanceAnalysisService.getPerformance(
                null, AnalysisEntityType.PORTFOLIO, "1D", "user-a");
        PerformanceResponse userB = performanceAnalysisService.getPerformance(
                null, AnalysisEntityType.PORTFOLIO, "1D", "user-b");

        assertNotEquals(userA.getTotalReturnPercentage(), userB.getTotalReturnPercentage());
    }

    @Test
    void aggregatedPerformance_virtualEntityIdIsUserScoped() {
        when(entityLoadService.loadPortfoliosForUser(eq("user-a"), eq(BootstrapTrigger.HTTP_READ)))
                .thenReturn(entityLoadResult("user-a", 100000.0));

        performanceAnalysisService.getPerformance(null, AnalysisEntityType.PORTFOLIO, "1D", "user-a");

        org.mockito.Mockito.verify(calculationService).calculatePerformance(
                org.mockito.ArgumentMatchers.argThat(entity ->
                        "user-a".equals(entity.getOwnerId())
                                && "VIRTUAL_ALL_user-a".equals(entity.getId())
                                && "ALL".equals(entity.getSourceId())),
                eq("1D"));
    }

    private EntityLoadResult entityLoadResult(String userId, double totalValue) {
        AnalysisEntity portfolio = AnalysisEntity.builder()
                .id("PORTFOLIO_" + userId)
                .sourceId("broker-" + userId)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(userId)
                .performance(PerformanceSummary.builder()
                        .totalValue(totalValue)
                        .totalInvestment(totalValue * 0.9)
                        .build())
                .build();
        return EntityLoadResult.of(List.of(portfolio), false);
    }
}
