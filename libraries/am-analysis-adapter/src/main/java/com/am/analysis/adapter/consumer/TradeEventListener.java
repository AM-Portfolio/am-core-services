package com.am.analysis.adapter.consumer;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.service.AnalysisIngestionService;
import am.trade.kafka.model.TradeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TradeEventListener {

    private final AnalysisEventMapper mapper;
    private final AnalysisIngestionService ingestionService;

    @KafkaListener(topics = "trade-updates", groupId = "am-analysis-group")
    public void listen(TradeEvent event) {
        log.info("Received Trade Event: {}", event.getTradeId());
        try {
            var entity = mapper.mapTradeEvent(event);
            ingestionService.ingest(entity);
        } catch (Exception e) {
            log.error("Failed to process trade event", e);
        }
    }
}
