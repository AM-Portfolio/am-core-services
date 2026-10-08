package com.am.analysis.adapter.service;

import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.kafka.config.AnalysisEntityKeys;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisIngestionServiceDeleteTest {

    private static final String USER_ID = "user-1";
    private static final String PORTFOLIO_ID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private AnalysisRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AnalysisIngestionService service;

    @BeforeEach
    void setUp() {
        service = new AnalysisIngestionService(repository, eventPublisher);
    }

    @Test
    void deletePortfolio_removesKeyedEntity() {
        String entityId = AnalysisEntityKeys.portfolioEntityId(PORTFOLIO_ID, USER_ID);
        when(repository.existsById(entityId)).thenReturn(true);

        service.deletePortfolio(PortfolioUpdateEvent.builder()
                .userId(USER_ID)
                .portfolioId(PORTFOLIO_ID)
                .action("DELETE")
                .build());

        verify(repository).deleteById(entityId);
    }

    @Test
    void deletePortfolio_missingEntity_isNoOp() {
        String entityId = AnalysisEntityKeys.portfolioEntityId(PORTFOLIO_ID, USER_ID);
        when(repository.existsById(entityId)).thenReturn(false);

        service.deletePortfolio(PortfolioUpdateEvent.builder()
                .userId(USER_ID)
                .portfolioId(PORTFOLIO_ID)
                .action("DELETE")
                .build());

        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void deletePortfolio_blankPortfolioId_doesNotTouchGlobal() {
        service.deletePortfolio(PortfolioUpdateEvent.builder()
                .userId(USER_ID)
                .portfolioId("  ")
                .action("DELETE")
                .build());

        verifyNoInteractions(repository);
    }

    @Test
    void deletePortfolio_nullPortfolioId_doesNotTouchGlobal() {
        service.deletePortfolio(PortfolioUpdateEvent.builder()
                .userId(USER_ID)
                .action("DELETE")
                .build());

        verifyNoInteractions(repository);
    }

    @Test
    void deletePortfolio_globalSourceId_isRefused() {
        service.deletePortfolio(PortfolioUpdateEvent.builder()
                .userId(USER_ID)
                .portfolioId(AnalysisEntityKeys.GLOBAL_SOURCE_ID)
                .action("DELETE")
                .build());

        verifyNoInteractions(repository);
    }

    @Test
    void deletePortfolio_missingUserId_isNoOp() {
        service.deletePortfolio(PortfolioUpdateEvent.builder()
                .portfolioId(PORTFOLIO_ID)
                .action("DELETE")
                .build());

        verifyNoInteractions(repository);
    }
}
