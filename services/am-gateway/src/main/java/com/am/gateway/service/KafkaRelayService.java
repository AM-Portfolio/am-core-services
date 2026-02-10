package com.am.gateway.service;

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

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.STOCK_UPDATE, groupId = "am-websocket-gateway-group")
    public void handleStockUpdate(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("symbol")) {
                String symbol = node.get("symbol").asText();
                // Route to specific stock topic: /topic/stock/{symbol}
                messagingTemplate.convertAndSend("/topic/stock/" + symbol, message);
            } else {
                log.warn("Invalid Stock Update: Missing 'symbol' field. Message: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to parse Stock Update: {}", message, e);
        }
    }

    private final com.am.portfolio.domain.mapper.PortfolioGenericMapper genericMapper = new com.am.portfolio.domain.mapper.PortfolioGenericMapper();

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.PORTFOLIO_UPDATE, groupId = "am-websocket-gateway-group")
    public void handlePortfolioStreamUpdate(String message) {
        // 1. Entry Log - Immediate visibility
        log.info("[Relay] Received Portfolio Update. Length: {}", message.length());

        try {
            // 2. Deserialization
            com.am.portfolio.domain.events.PortfolioUpdateEvent event = objectMapper.readValue(message,
                    com.am.portfolio.domain.events.PortfolioUpdateEvent.class);

            if (event.getUserId() != null) {
                String userId = event.getUserId();
                String portfolioId = event.getPortfolioId(); // Extract portfolioId

                // 3. Processing Log with portfolioId
                if (portfolioId != null && !portfolioId.isEmpty()) {
                    log.info("[Relay] Processing for User: {}, PortfolioID: {}", userId, portfolioId);
                } else {
                    log.info("[Relay] Processing for User: {} (all portfolios)", userId);
                }

                // 4. Transform using common mapper from adapter
                PortfolioUpdateDto optimizedPayload = genericMapper.mapToDto(event);

                int holdingCount = (optimizedPayload.getEquities() != null) ? optimizedPayload.getEquities().size() : 0;

                // 5. Dispatch
                try {
                    if (portfolioId != null && !portfolioId.isEmpty()) {
                        log.info(
                                "[Relay] Attempting WebSocket send - User: {}, PortfolioID: {}, Destination: /queue/portfolio, Holdings: {}",
                                userId, portfolioId, holdingCount);
                    } else {
                        log.info(
                                "[Relay] Attempting WebSocket send - User: {}, Destination: /queue/portfolio, Holdings: {} (all portfolios)",
                                userId, holdingCount);
                    }

                    messagingTemplate.convertAndSendToUser(userId, "/queue/portfolio", optimizedPayload);

                    if (portfolioId != null && !portfolioId.isEmpty()) {
                        log.info("[Relay] ✅ WebSocket send COMPLETED for User: {}, PortfolioID: {}, Holdings: {}",
                                userId, portfolioId, holdingCount);
                    } else {
                        log.info("[Relay] ✅ WebSocket send COMPLETED for User: {}, Holdings: {} (all portfolios)",
                                userId, holdingCount);
                    }
                } catch (Exception sendEx) {
                    log.error("[Relay] ❌ FAILED to send WebSocket message for User: {}", userId, sendEx);
                }

            } else {
                log.error("[Relay] Validation Error: Missing 'userId' in payload.");
            }
        } catch (Exception e) {
            log.error("[Relay] Critical Error processing Portfolio Update", e);
        }
    }

    @KafkaListener(topics = com.am.kafka.config.KafkaTopics.TRADE_UPDATE, groupId = "am-websocket-gateway-group")
    public void handleTradeUpdate(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            if (node.has("userId")) {
                String userId = node.get("userId").asText();
                // Route to specific user trade updates
                messagingTemplate.convertAndSendToUser(userId, "/queue/trade", message);
            } else {
                log.warn("Invalid Trade Update: Missing 'userId' field. Message: {}", message);
            }
        } catch (Exception e) {
            log.error("Failed to parse Trade Update: {}", message, e);
        }
    }
}
