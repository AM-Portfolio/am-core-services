package com.am.gateway.controller;

import com.am.gateway.service.PortfolioSubscriptionManager;
import com.am.kafka.config.InterestRegistryKeys;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP {@code /app/dashboard/*} — dashboard watch channel ({@link InterestRegistryKeys#CHANNEL_DASHBOARD_MAIN}).
 */
@Controller
@RequiredArgsConstructor
public class DashboardStompController {

    private final PortfolioSubscriptionManager subscriptionManager;
    private final StompPrincipalResolver principalResolver;
    private final FlowLogger flowLogger;

    @MessageMapping("/dashboard/subscribe")
    public void subscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = principalResolver.resolve(headerAccessor, payload);
        String sessionId = headerAccessor.getSessionId();

        try (FlowSpan span = flowLogger.start("gateway.stomp.dashboard.subscribe.received",
                "userId", userId,
                "sessionId", sessionId)) {

            subscriptionManager.onSubscribe(userId, InterestRegistryKeys.CHANNEL_DASHBOARD_MAIN, sessionId);
            flowLogger.complete(span);
        }
    }

    @MessageMapping("/dashboard/unsubscribe")
    public void unsubscribe(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        String userId = principalResolver.resolve(headerAccessor, payload);
        String sessionId = headerAccessor.getSessionId();
        subscriptionManager.onDisconnect(userId, sessionId);
    }
}
