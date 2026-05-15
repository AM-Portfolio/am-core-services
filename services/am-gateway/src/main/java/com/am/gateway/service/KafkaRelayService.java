package com.am.gateway.service;

import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.sanitize.Sanitizer;
import com.am.portfolio.domain.dto.PortfolioUpdateDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaRelayService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final com.am.analysis.adapter.mapper.AnalysisEventMapper analysisEventMapper;
    private final FlowLogger flowLogger;
    private final Sanitizer sanitizer;

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.STOCK_UPDATE, groupId = "am-websocket-gateway-group")
    public void handleStockUpdate(String message) {
        try (FlowSpan span = flowLogger.start("gateway.kafka.relay.stock_update",
                "payload_bytes", message == null ? 0 : message.length())) {
            try {
                com.am.common.investment.model.events.EquityPriceUpdateEvent event =
                        objectMapper.readValue(message, com.am.common.investment.model.events.EquityPriceUpdateEvent.class);

                int count = 0;
                if (event.getEquityPrices() != null) {
                    for (var price : event.getEquityPrices()) {
                        String symbol = price.getSymbol();
                        String priceJson = objectMapper.writeValueAsString(price);
                        messagingTemplate.convertAndSend("/topic/stock/" + symbol, priceJson);
                        count++;
                    }
                }
                flowLogger.complete(span, "prices", count);
            } catch (Exception e) {
                log.debug("Stock update parse failed, preview={}", sanitizer.preview(message));
                flowLogger.fail(span, e);
            }
        }
    }

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.PORTFOLIO_UPDATE, groupId = "am-websocket-gateway-group")
    public void handlePortfolioStreamUpdate(String message) {
        try (FlowSpan span = flowLogger.start("gateway.kafka.relay.portfolio_update",
                "payload_bytes", message == null ? 0 : message.length())) {
            try {
                com.am.portfolio.domain.events.PortfolioUpdateEvent event = objectMapper.readValue(message,
                        com.am.portfolio.domain.events.PortfolioUpdateEvent.class);

                if (event.getUserId() == null) {
                    flowLogger.fail(span, null, "reason", "missing_userId");
                    return;
                }
                String userId = event.getUserId();
                String portfolioId = event.getPortfolioId();

                PortfolioUpdateDto optimizedPayload = analysisEventMapper.mapToDto(event);
                int holdingCount = (optimizedPayload.getEquities() != null) ? optimizedPayload.getEquities().size() : 0;

                flowLogger.step("gateway.ws.send.queue_portfolio",
                        "userId", userId,
                        "portfolioId", portfolioId != null ? portfolioId : "ALL",
                        "holdings", holdingCount);
                messagingTemplate.convertAndSendToUser(userId, "/queue/portfolio", optimizedPayload);
                flowLogger.complete(span,
                        "userId", userId,
                        "portfolioId", portfolioId != null ? portfolioId : "ALL",
                        "holdings", holdingCount);
            } catch (Exception e) {
                log.debug("Portfolio update parse/dispatch failed, preview={}", sanitizer.preview(message));
                flowLogger.fail(span, e);
            }
        }
    }

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.TRADE_UPDATE, groupId = "am-websocket-gateway-group")
    public void handleTradeUpdate(String message) {
        try (FlowSpan span = flowLogger.start("gateway.kafka.relay.trade_update",
                "payload_bytes", message == null ? 0 : message.length())) {
            try {
                JsonNode node = objectMapper.readTree(message);
                if (!node.has("userId")) {
                    flowLogger.fail(span, null, "reason", "missing_userId");
                    log.debug("Trade update preview={}", sanitizer.preview(message));
                    return;
                }
                String userId = node.get("userId").asText();
                messagingTemplate.convertAndSendToUser(userId, "/queue/trade", message);
                flowLogger.complete(span, "userId", userId);
            } catch (Exception e) {
                log.debug("Trade update parse failed, preview={}", sanitizer.preview(message));
                flowLogger.fail(span, e);
            }
        }
    }

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.DASHBOARD_UPDATE, groupId = "am-websocket-gateway-group")
    public void handleDashboardUpdate(String message) {
        try (FlowSpan span = flowLogger.start("gateway.kafka.relay.dashboard_update",
                "payload_bytes", message == null ? 0 : message.length())) {
            try {
                JsonNode node = objectMapper.readTree(message);
                if (!node.has("userId")) {
                    flowLogger.fail(span, null, "reason", "missing_userId");
                    return;
                }
                String userId = node.get("userId").asText();
                messagingTemplate.convertAndSend("/topic/dashboard/" + userId, message);
                flowLogger.complete(span, "userId", userId);
            } catch (Exception e) {
                log.debug("Dashboard update parse failed, preview={}", sanitizer.preview(message));
                flowLogger.fail(span, e);
            }
        }
    }
}
