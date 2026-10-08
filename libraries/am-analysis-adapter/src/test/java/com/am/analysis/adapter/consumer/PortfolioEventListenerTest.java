package com.am.analysis.adapter.consumer;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.service.AnalysisIngestionService;
import com.am.portfolio.domain.events.PortfolioUpdateEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

        ArgumentCaptor<PortfolioUpdateEvent> cap = ArgumentCaptor.forClass(PortfolioUpdateEvent.class);
        verify(ingestionService).deletePortfolio(cap.capture());
        assertEquals("p-1", cap.getValue().getPortfolioId());
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

        verify(ingestionService).deletePortfolio(any(PortfolioUpdateEvent.class));
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
        verify(ingestionService, never()).deletePortfolio(any());
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

        verify(ingestionService).ingest(entity);
        verify(ingestionService, never()).deletePortfolio(any());
    }
}
