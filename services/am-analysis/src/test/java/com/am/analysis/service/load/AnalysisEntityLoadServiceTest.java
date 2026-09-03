package com.am.analysis.service.load;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.analysis.service.bootstrap.PortfolioBootstrapTrigger;
import com.am.analysis.service.validator.AnalysisAccessValidator;
import com.am.observability.flow.FlowLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisEntityLoadServiceTest {

    @Mock
    private AnalysisRepository repository;
    @Mock
    private AnalysisAccessValidator accessValidator;
    @Mock
    private PortfolioBootstrapTrigger portfolioBootstrapTrigger;
    @Mock
    private FlowLogger flowLogger;

    @Mock
    private StringRedisTemplate redisTemplate;

    private AnalysisEntityLoadService service;

    @BeforeEach
    void setUp() {
        service = new AnalysisEntityLoadService(repository, accessValidator, portfolioBootstrapTrigger, flowLogger, redisTemplate);
    }

    @Test
    void loadPortfoliosForUser_returnsEntitiesWhenPresent() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .id("PORTFOLIO_p1")
                .sourceId("p1")
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user1")
                .build();
        when(repository.findByOwnerIdAndType("user1", AnalysisEntityType.PORTFOLIO))
                .thenReturn(List.of(entity));

        EntityLoadResult result = service.loadPortfoliosForUser("user1", BootstrapTrigger.HTTP_READ);

        assertFalse(result.empty());
        assertEquals(1, result.entities().size());
        assertFalse(result.bootstrapRequested());
        verify(portfolioBootstrapTrigger, never()).requestBootstrap(any(), any(), any(), any());
    }

    @Test
    void loadPortfoliosForUser_firesBootstrapWhenEmpty() {
        when(repository.findByOwnerIdAndType("user1", AnalysisEntityType.PORTFOLIO))
                .thenReturn(List.of());
        when(portfolioBootstrapTrigger.requestBootstrap(eq("user1"), isNull(), anyString(), isNull()))
                .thenReturn(true);

        EntityLoadResult result = service.loadPortfoliosForUser("user1", BootstrapTrigger.HTTP_READ);

        assertTrue(result.empty());
        assertTrue(result.bootstrapRequested());
        verify(portfolioBootstrapTrigger).requestBootstrap(eq("user1"), isNull(), eq("BOOTSTRAP_HTTP_READ"), isNull());
    }

    @Test
    void loadPortfoliosForUser_excludesGlobalEntities() {
        AnalysisEntity global = AnalysisEntity.builder()
                .id("PORTFOLIO_GLOBAL_user1")
                .sourceId("GLOBAL")
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user1")
                .build();
        when(repository.findByOwnerIdAndType("user1", AnalysisEntityType.PORTFOLIO))
                .thenReturn(List.of(global));
        when(portfolioBootstrapTrigger.requestBootstrap(any(), any(), any(), any())).thenReturn(true);

        EntityLoadResult result = service.loadPortfoliosForUser("user1", BootstrapTrigger.DASHBOARD);

        assertTrue(result.empty());
        verify(portfolioBootstrapTrigger).requestBootstrap(eq("user1"), isNull(), anyString(), isNull());
    }

    @Test
    void loadOne_returnsEntityWhenFound() {
        AnalysisEntity entity = AnalysisEntity.builder()
                .id("PORTFOLIO_p1")
                .sourceId("p1")
                .type(AnalysisEntityType.PORTFOLIO)
                .ownerId("user1")
                .build();
        when(repository.findById("PORTFOLIO_p1")).thenReturn(Optional.of(entity));
        doNothing().when(accessValidator).verifyAccess(entity, "user1");

        EntityLoadResult result = service.loadOne(
                EntityLoadRequest.onePortfolio("p1", "user1", BootstrapTrigger.HTTP_READ));

        assertFalse(result.empty());
        assertEquals("p1", result.entities().get(0).getSourceId());
    }

    @Test
    void loadOne_firesBootstrapWhenMissing() {
        when(repository.findById("PORTFOLIO_p1")).thenReturn(Optional.empty());
        when(portfolioBootstrapTrigger.requestBootstrap(eq("user1"), eq("p1"), anyString(), isNull()))
                .thenReturn(true);

        EntityLoadResult result = service.loadOne(
                EntityLoadRequest.onePortfolio("p1", "user1", BootstrapTrigger.HTTP_READ));

        assertTrue(result.empty());
        assertTrue(result.bootstrapRequested());
    }

    @Test
    void loadGlobalPortfolio_returnsMatchingOwner() {
        AnalysisEntity global = AnalysisEntity.builder()
                .id("PORTFOLIO_GLOBAL_user1")
                .ownerId("user1")
                .build();
        when(repository.findById("PORTFOLIO_GLOBAL_user1")).thenReturn(Optional.of(global));

        assertTrue(service.loadGlobalPortfolio("user1").isPresent());
    }
}
