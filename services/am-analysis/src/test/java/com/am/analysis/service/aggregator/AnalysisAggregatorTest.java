package com.am.analysis.service.aggregator;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.dto.DashboardSummary;
import com.am.domain.trade.TradePortfolio;
import com.am.trade.client.service.TradeClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalysisAggregatorTest {

    @Mock
    private AnalysisRepository analysisRepository;

    @Mock
    private TradeClientService tradeClientService;

    @InjectMocks
    private AnalysisAggregator aggregator;

    @Test
    void testGetOverallSummary() {
        // Setup AM Portfolio
        AnalysisEntity amPortfolio = new AnalysisEntity();
        amPortfolio.setPerformance(PerformanceSummary.builder()
                .totalValue(10000.0)
                .totalInvestment(8000.0)
                .dayChange(500.0)
                .build());

        // Setup Trade Portfolio
        TradePortfolio tradePortfolio = TradePortfolio.builder()
                .totalValue(new BigDecimal("5000.0"))
                .totalInvested(new BigDecimal("4000.0"))
                .currentPnl(new BigDecimal("1000.0"))
                .build();

        when(analysisRepository.findByOwnerIdAndType(eq("user1"), any(AnalysisEntityType.class)))
                .thenReturn(List.of(amPortfolio));
        when(tradeClientService.getPortfolios("user1"))
                .thenReturn(List.of(tradePortfolio));

        // Execute
        DashboardSummary summary = aggregator.getOverallSummary("user1");

        // Verify
        assertEquals(new BigDecimal("15000.0"), summary.getTotalValue()); // 10000 + 5000
        assertEquals(new BigDecimal("12000.0"), summary.getTotalInvested()); // 8000 + 4000
        assertEquals(new BigDecimal("3000.0"), summary.getTotalGainLoss()); // 15000 - 12000
        assertEquals(25.0, summary.getTotalGainLossPercentage()); // 3000 / 12000 * 100
        assertEquals(new BigDecimal("500.0"), summary.getDayChange()); // 500 + 0 (trade assumed 0)
    }
}
