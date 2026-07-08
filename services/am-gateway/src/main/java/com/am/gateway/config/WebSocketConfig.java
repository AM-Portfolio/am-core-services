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
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

/**
 * Centralized WebSocket Configuration for AM Gateway.
 *
 * This class handles:
 *  1. STOMP endpoint registration (with SockJS fallback).
 *  2. Message broker configuration (topic/queue/user).
 *  3. Inbound channel interception for Security (JWT validation) and Tracing.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompTracingChannelInterceptor stompTracingInterceptor;
    private final FlowLogger flowLogger;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS endpoint (fallback for browsers that do not support WebSocket)
        registry.addEndpoint("/v1/streams-fallback", "/ws-gateway-fallback")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket endpoint (enterprise standard)
        // Adding /ws-gateway-raw as an alias for backward compatibility with Postman docs
        registry.addEndpoint("/v1/streams", "/ws-gateway-raw")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple memory-based message broker
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        
        // Prefix for messages bound for methods annotated with @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific queues
        registry.setUserDestinationPrefix("/user");
    }

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

                    // 1. Extract Token (Support both Header and Query Param for flexibility)
                    String token = extractToken(accessor);
                    
                    if (token == null) {
                        flowLogger.fail(span, null, "reason", "missing_authorization");
                        log.warn("STOMP Connect rejected: Missing Authorization for session {}", sessionId);
                        throw new IllegalArgumentException("Unauthorized: Missing token");
                    }

                    // 2. Validate Token & Extract User
                    try {
                        String userId = com.am.security.util.TokenExtractor.extractUserId(token);
                        if (userId == null) {
                            throw new RuntimeException("Token valid but missing sub/userId");
                        }

                        // 3. Bind User to Session
                        Principal userPrincipal = () -> userId;
                        accessor.setUser(userPrincipal);
                        
                        flowLogger.complete(span, "userId", userId);
                        log.info("STOMP Session {} authenticated for User {}", sessionId, userId);
                        return message;

                    } catch (Exception e) {
                        flowLogger.fail(span, e, "reason", "invalid_token");
                        log.error("STOMP Connect rejected: Invalid token for session {}. Error: {}", sessionId, e.getMessage());
                        throw new IllegalArgumentException("Unauthorized: Invalid token");
                    }
                }
            }
        });
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization Header
        List<String> authHeader = accessor.getNativeHeader("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            String val = authHeader.get(0);
            return val.startsWith("Bearer ") ? val.substring(7) : val;
        }

        // Fallback: Try token query parameter (some clients can't send headers on WS connect)
        List<String> tokenParam = accessor.getNativeHeader("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam.get(0);
        }

        return null;
    }
}
