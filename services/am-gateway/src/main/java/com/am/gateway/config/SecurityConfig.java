package com.am.gateway.config;

import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.stomp.StompTracingChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final StompTracingChannelInterceptor stompTracingInterceptor;
    private final FlowLogger flowLogger;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Tracing interceptor runs first so MDC is populated for the security check below.
        registration.interceptors(stompTracingInterceptor, new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                String sessionId = accessor.getSessionId();
                try (FlowSpan span = flowLogger.start("gateway.stomp.connect",
                        "sessionId", sessionId)) {

                    List<String> authHeader = accessor.getNativeHeader("Authorization");
                    if (authHeader == null || authHeader.isEmpty()) {
                        flowLogger.fail(span, null, "reason", "missing_authorization");
                        throw new IllegalArgumentException("Unauthorized");
                    }
                    String token = authHeader.get(0);
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }

                    io.jsonwebtoken.Claims claims;
                    try {
                        claims = com.am.security.util.TokenExtractor.extractClaims(token);
                    } catch (Exception e) {
                        flowLogger.fail(span, null, "reason", "invalid_token");
                        throw new IllegalArgumentException("Unauthorized");
                    }

                    String userId = com.am.security.util.TokenExtractor.extractUserId(token);
                    if (userId == null) {
                        flowLogger.fail(span, null, "reason", "token_missing_user");
                        throw new IllegalArgumentException("Unauthorized");
                    }

                    Principal userPrincipal = () -> userId;
                    accessor.setUser(userPrincipal);
                    flowLogger.complete(span, "userId", userId);
                    return message;
                }
            }
        });
    }
}


// test