package com.am.gateway.controller;

import com.am.gateway.service.GatewayKafkaProducer;
import com.am.gateway.service.PortfolioSubscriptionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {

    private final GatewayKafkaProducer gatewayKafkaProducer;
    private final PortfolioSubscriptionManager subscriptionManager;

    @MessageMapping("/portfolio/calculate")
    public void triggerCalculation(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        // Log the RAW payload for debugging
        log.info("📥 RAW WebSocket Payload: {}", payload);

        String userId = payload.get("userId");
        if (userId == null) {
            log.warn("Calculation request missing userId");
            return;
        }

        // Extract optional portfolioId (hybrid approach)
        String portfolioId = payload.get("portfolioId");

        // Extract Correlation ID from STOMP headers
        String correlationId = (String) headerAccessor.getFirstNativeHeader("X-Correlation-Id");
        if (correlationId == null) {
            correlationId = java.util.UUID.randomUUID().toString();
            log.info("Generated new Correlation ID: {}", correlationId);
        }

        // Update the subscription manager with the selected portfolio if present
        if (portfolioId != null && !portfolioId.isEmpty()) {
            subscriptionManager.setUserPortfolio(userId, portfolioId);
        }

        // Use the centralized producer to send the trigger
        gatewayKafkaProducer.sendCalculationTrigger(userId, portfolioId, correlationId, "MANUAL_WEBSOCKET");
    }
}
