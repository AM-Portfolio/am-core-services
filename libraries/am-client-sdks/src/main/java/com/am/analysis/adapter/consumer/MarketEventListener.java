package com.am.analysis.adapter.consumer;

import com.am.analysis.adapter.mapper.AnalysisEventMapper;
import com.am.analysis.adapter.service.AnalysisIngestionService;
import com.am.common.investment.model.events.EquityPriceUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "am.analysis.adapter.market", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MarketEventListener {

    private final AnalysisEventMapper mapper;
    private final AnalysisIngestionService ingestionService;

    @KafkaListener(topics = "equity-price-updates", groupId = "am-analysis-group")
    public void listen(EquityPriceUpdateEvent event) {
        log.info("Received Market Event with {} updates", event.getEquityPrices().size());
        try {
            var entities = mapper.mapMarketEvent(event);
            entities.forEach(ingestionService::ingest);
        } catch (Exception e) {
            log.error("Failed to process market event", e);
        }
    }
}
