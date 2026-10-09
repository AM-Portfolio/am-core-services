package com.am.analysis.adapter.consumer;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.service.AnalysisIngestionService;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioEventListenerTest {

    @Mock
    private AnalysisEventMapper mapper;
    @Mock
    private AnalysisIngestionService ingestionService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private PortfolioEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new PortfolioEventListener(mapper, ingestionService, objectMapper);
    }

    @Test
    void listen_deleteAction_callsDeleteNotIngest() throws Exception {
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
                .userId("user-1")
                .portfolioId("p-1")
                .action("DELETE")
                .build();

        listener.listen(objectMapper.writeValueAsString(event));

        verify(ingestionService).delete("p-1", "user-1");
        verify(ingestionService, never()).ingest(any());
        verifyNoInteractions(mapper);
    }

    @Test
    void listen_deleteActionCaseAndWhitespace_callsDelete() throws Exception {
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
                .userId("user-1")
                .portfolioId("p-1")
                .action(" delete ")
                .build();

        listener.listen(objectMapper.writeValueAsString(event));

        verify(ingestionService).delete("p-1", "user-1");
        verify(ingestionService, never()).ingest(any());
    }

    @Test
    void listen_nullAction_ingestsAsBefore() throws Exception {
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
                .userId("user-1")
                .portfolioId("p-1")
                .build();
        AnalysisEntity entity = AnalysisEntity.builder().id("PORTFOLIO_p-1").sourceId("p-1").build();
        when(mapper.mapPortfolioEvent(any(PortfolioUpdateEvent.class))).thenReturn(entity);

        listener.listen(objectMapper.writeValueAsString(event));

        verify(mapper).mapPortfolioEvent(any(PortfolioUpdateEvent.class));
        verify(ingestionService).ingest(entity);
        verify(ingestionService, never()).delete(any(), any());
    }

    @Test
    void listen_blankAction_ingestsAsBefore() throws Exception {
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
                .userId("user-1")
                .portfolioId("p-1")
                .action("   ")
                .build();
        AnalysisEntity entity = AnalysisEntity.builder().id("PORTFOLIO_p-1").sourceId("p-1").build();
        when(mapper.mapPortfolioEvent(any(PortfolioUpdateEvent.class))).thenReturn(entity);

        listener.listen(objectMapper.writeValueAsString(event));

        verify(ingestionService).ingest(entity);
        verify(ingestionService, never()).delete(any(), any());
    }

    @Test
    void listen_updateAction_ingests() throws Exception {
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
                .userId("user-1")
                .portfolioId("p-1")
                .action("UPDATE")
                .build();
        AnalysisEntity entity = AnalysisEntity.builder().id("PORTFOLIO_p-1").sourceId("p-1").build();
        when(mapper.mapPortfolioEvent(any(PortfolioUpdateEvent.class))).thenReturn(entity);

        listener.listen(objectMapper.writeValueAsString(event));

        verify(ingestionService).ingest(eq(entity));
        verify(ingestionService, never()).delete(any(), any());
    }

    @Test
    void listen_createAction_ingests() throws Exception {
        PortfolioUpdateEvent event = PortfolioUpdateEvent.builder()
                .userId("user-1")
                .portfolioId("p-1")
                .action("CREATE")
                .build();
        AnalysisEntity entity = AnalysisEntity.builder().id("PORTFOLIO_p-1").sourceId("p-1").build();
        when(mapper.mapPortfolioEvent(any(PortfolioUpdateEvent.class))).thenReturn(entity);

        listener.listen(objectMapper.writeValueAsString(event));

        verify(ingestionService).ingest(entity);
        verify(ingestionService, never()).delete(any(), any());
    }

    @Test
    void listen_invalidJson_swallowsException() {
        listener.listen("{not-json");

        verifyNoInteractions(mapper, ingestionService);
    }
}
