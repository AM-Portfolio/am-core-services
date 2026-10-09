package com.am.analysis.service.listener;

import com.am.analysis.service.DashboardAnalysisService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardUpdateListenerTest {

    @Mock
    private DashboardAnalysisService dashboardService;
    @Mock
    private FlowLogger flowLogger;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DashboardUpdateListener listener;
    private FlowSpan span;

    @BeforeEach
    void setUp() {
        listener = new DashboardUpdateListener(dashboardService, objectMapper, flowLogger);
        span = mock(FlowSpan.class);
        when(flowLogger.start(anyString(), any(), any())).thenReturn(span);
    }

    @Test
    void onPortfolioUpdate_updateAction_refreshesDashboard() {
        listener.onPortfolioUpdate("{\"userId\":\"user-1\",\"action\":\"UPDATE\",\"portfolioId\":\"p-1\"}");

        verify(dashboardService).publishDashboardUpdate("user-1");
        verify(flowLogger).complete(eq(span), eq("userId"), eq("user-1"));
    }

    @Test
    void onPortfolioUpdate_missingAction_refreshesDashboard() {
        listener.onPortfolioUpdate("{\"userId\":\"user-1\",\"portfolioId\":\"p-1\"}");

        verify(dashboardService).publishDashboardUpdate("user-1");
    }

    @Test
    void onPortfolioUpdate_deleteAction_skipsRefresh() {
        listener.onPortfolioUpdate("{\"userId\":\"user-1\",\"action\":\"DELETE\",\"portfolioId\":\"p-1\"}");

        verify(dashboardService, never()).publishDashboardUpdate(anyString());
        verify(flowLogger).complete(eq(span), eq("userId"), eq("user-1"), eq("skipped"), eq("DELETE"));
    }

    @Test
    void onPortfolioUpdate_deleteActionCaseInsensitive_skipsRefresh() {
        listener.onPortfolioUpdate("{\"userId\":\"user-1\",\"action\":\"delete\",\"portfolioId\":\"p-1\"}");

        verify(dashboardService, never()).publishDashboardUpdate(anyString());
    }

    @Test
    void onPortfolioUpdate_missingUserId_skipsRefresh() {
        listener.onPortfolioUpdate("{\"action\":\"UPDATE\",\"portfolioId\":\"p-1\"}");

        verify(dashboardService, never()).publishDashboardUpdate(anyString());
        verify(flowLogger).fail(eq(span), eq(null), eq("reason"), eq("missing_userId"));
    }
}
