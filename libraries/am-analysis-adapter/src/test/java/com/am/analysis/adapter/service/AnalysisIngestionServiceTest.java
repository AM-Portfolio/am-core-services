package com.am.analysis.adapter.service;

import com.am.analysis.adapter.event.AnalysisEntityIngestedEvent;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.kafka.config.AnalysisEntityKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisIngestionServiceTest {

    private static final String USER_ID = "user-1";
    private static final String PORTFOLIO_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private AnalysisRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private DashboardRefreshPort dashboardRefreshPort;

    private AnalysisIngestionService service;

    @BeforeEach
    void setUp() {
        service = new AnalysisIngestionService(repository, eventPublisher);
        ReflectionTestUtils.setField(service, "dashboardRefreshPort", dashboardRefreshPort);
    }

    @Test
    void ingest_savesPublishesEventAndRefreshesDashboard() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .id("PORTFOLIO_" + PORTFOLIO_ID)
                .sourceId(PORTFOLIO_ID)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(USER_ID)
                .build();

        service.ingest(entity);

        verify(repository).save(entity);
        verify(eventPublisher).publishEvent(any(AnalysisEntityIngestedEvent.class));
        verify(dashboardRefreshPort, timeout(2000)).publishDashboardUpdate(USER_ID);
    }

    @Test
    void ingest_blankOwnerId_skipsDashboardRefresh() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .id("PORTFOLIO_" + PORTFOLIO_ID)
                .sourceId(PORTFOLIO_ID)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(" ")
                .build();

        service.ingest(entity);

        verify(repository).save(entity);
        verify(eventPublisher).publishEvent(any(AnalysisEntityIngestedEvent.class));
        verify(dashboardRefreshPort, never()).publishDashboardUpdate(anyString());
    }

    @Test
    void ingest_nullRefreshPort_doesNotThrow() {
        ReflectionTestUtils.setField(service, "dashboardRefreshPort", null);
        AnalysisEntity entity = AnalysisEntity.builder()
                .id("PORTFOLIO_" + PORTFOLIO_ID)
                .sourceId(PORTFOLIO_ID)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(USER_ID)
                .build();

        service.ingest(entity);

        verify(repository).save(entity);
        verify(eventPublisher).publishEvent(any(AnalysisEntityIngestedEvent.class));
    }

    @Test
    void delete_removesPortfolioEntityAndRefreshesDashboard() {
        String entityId = AnalysisEntityKeys.portfolioEntityId(PORTFOLIO_ID, USER_ID);
        AnalysisEntity entity = AnalysisEntity.builder()
                .id(entityId)
                .sourceId(PORTFOLIO_ID)
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId(USER_ID)
                .build();
        when(repository.findById(entityId)).thenReturn(Optional.of(entity));

        service.delete(PORTFOLIO_ID, USER_ID);

        assertEquals("PORTFOLIO_" + PORTFOLIO_ID, entityId);
        verify(repository).delete(entity);
        verify(dashboardRefreshPort, timeout(2000)).publishDashboardUpdate(USER_ID);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void delete_whenEntityMissing_stillRefreshesDashboard() {
        String entityId = AnalysisEntityKeys.portfolioEntityId(PORTFOLIO_ID, USER_ID);
        when(repository.findById(entityId)).thenReturn(Optional.empty());

        service.delete(PORTFOLIO_ID, USER_ID);

        verify(repository, never()).delete(any());
        verify(dashboardRefreshPort, timeout(2000)).publishDashboardUpdate(USER_ID);
    }

    @Test
    void delete_nullPortfolioId_isNoOp() {
        service.delete(null, USER_ID);

        verifyNoInteractions(repository);
        verify(dashboardRefreshPort, never()).publishDashboardUpdate(anyString());
    }

    @Test
    void delete_blankPortfolioId_isNoOp() {
        service.delete("  ", USER_ID);

        verifyNoInteractions(repository);
        verify(dashboardRefreshPort, never()).publishDashboardUpdate(anyString());
    }

    @Test
    void delete_globalPortfolioId_isNoOp() {
        service.delete(AnalysisEntityKeys.GLOBAL_SOURCE_ID, USER_ID);

        verifyNoInteractions(repository);
        verify(dashboardRefreshPort, never()).publishDashboardUpdate(anyString());
    }

    @Test
    void delete_allAlias_isNoOp() {
        service.delete("all", USER_ID);

        verifyNoInteractions(repository);
        verify(dashboardRefreshPort, never()).publishDashboardUpdate(anyString());
    }

    @Test
    void delete_blankUserId_deletesButSkipsRefresh() {
        String entityId = AnalysisEntityKeys.portfolioEntityId(PORTFOLIO_ID, " ");
        AnalysisEntity entity = AnalysisEntity.builder()
                .id(entityId)
                .sourceId(PORTFOLIO_ID)
                .type(AnalysisEntityType.PORTFOLIO)
                .build();
        when(repository.findById(entityId)).thenReturn(Optional.of(entity));

        service.delete(PORTFOLIO_ID, " ");

        verify(repository).delete(entity);
        verify(dashboardRefreshPort, never()).publishDashboardUpdate(anyString());
    }

    @Test
    void delete_nullRefreshPort_doesNotThrow() {
        ReflectionTestUtils.setField(service, "dashboardRefreshPort", null);
        String entityId = AnalysisEntityKeys.portfolioEntityId(PORTFOLIO_ID, USER_ID);
        when(repository.findById(entityId)).thenReturn(Optional.empty());

        service.delete(PORTFOLIO_ID, USER_ID);

        verify(repository, never()).delete(any());
    }
}
