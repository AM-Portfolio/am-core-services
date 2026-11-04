package com.am.gateway.config;

import com.am.gateway.metrics.GatewayBusinessMetrics;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.stomp.StompTracingChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;

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

    private static final String ATTR_ACCESS_TOKEN = "access_token";
    private static final String ATTR_TOKEN = "token";

    private final StompTracingChannelInterceptor stompTracingInterceptor;
    private final FlowLogger flowLogger;
    private final GatewayBusinessMetrics businessMetrics;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        HandshakeInterceptor queryTokenInterceptor = new QueryTokenHandshakeInterceptor();

        // SockJS endpoint (fallback for browsers that do not support WebSocket)
        registry.addEndpoint("/v1/streams-fallback", "/ws-gateway-fallback")
                .addInterceptors(queryTokenInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket endpoint (enterprise standard)
        // Adding /ws-gateway-raw as an alias for backward compatibility with Postman docs
        registry.addEndpoint("/v1/streams", "/ws-gateway-raw")
                .addInterceptors(queryTokenInterceptor)
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
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    businessMetrics.sessionDisconnected();
                    return message;
                }

                if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                String sessionId = accessor.getSessionId();
                try (FlowSpan span = flowLogger.start("gateway.stomp.connect",
                        "sessionId", sessionId)) {

                    // 1. Extract Token (Support both Header and Query Param for flexibility)
                    String token = extractToken(accessor);
                    
                    if (token == null) {
                        businessMetrics.stompConnect("failure");
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

                        businessMetrics.stompConnect("success");
                        businessMetrics.sessionConnected();
                        flowLogger.complete(span, "userId", userId);
                        log.info("STOMP Session {} authenticated for User {}", sessionId, userId);
                        return message;

                    } catch (Exception e) {
                        businessMetrics.stompConnect("failure");
                        flowLogger.fail(span, e, "reason", "invalid_token");
                        log.error("STOMP Connect rejected: Invalid token for session {}. Error: {}", sessionId, e.getMessage());
                        throw new IllegalArgumentException("Unauthorized: Invalid token");
                    }
                }
            }
        });
    }

    /**
     * Resolve JWT from STOMP CONNECT headers, native headers, or handshake query
     * (browsers cannot set WS upgrade headers — Flutter web sends {@code access_token}).
     */
    static String extractToken(StompHeaderAccessor accessor) {
        List<String> authHeader = accessor.getNativeHeader("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            String val = authHeader.get(0);
            return stripBearer(val);
        }

        List<String> accessTokenHeader = accessor.getNativeHeader("access_token");
        if (accessTokenHeader != null && !accessTokenHeader.isEmpty()) {
            return stripBearer(accessTokenHeader.get(0));
        }

        List<String> tokenParam = accessor.getNativeHeader("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return stripBearer(tokenParam.get(0));
        }

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null) {
            Object fromAccess = attrs.get(ATTR_ACCESS_TOKEN);
            if (fromAccess instanceof String) {
                String s = (String) fromAccess;
                if (!s.isBlank()) {
                    return stripBearer(s);
                }
            }
            Object fromToken = attrs.get(ATTR_TOKEN);
            if (fromToken instanceof String) {
                String s = (String) fromToken;
                if (!s.isBlank()) {
                    return stripBearer(s);
                }
            }
        }

        return null;
    }

    private static String stripBearer(String val) {
        if (val == null) {
            return null;
        }
        String trimmed = val.trim();
        return trimmed.regionMatches(true, 0, "Bearer ", 0, 7)
                ? trimmed.substring(7).trim()
                : trimmed;
    }

    /**
     * Copies {@code access_token} / {@code token} query params into handshake attributes
     * so {@link #extractToken} can authenticate browser clients.
     */
    static final class QueryTokenHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            String query = request.getURI().getRawQuery();
            if (query == null || query.isBlank()) {
                return true;
            }
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = decode(pair.substring(0, eq));
                String value = decode(pair.substring(eq + 1));
                if (ATTR_ACCESS_TOKEN.equals(key) || ATTR_TOKEN.equals(key)) {
                    if (value != null && !value.isBlank()) {
                        attributes.put(key, value);
                    }
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception) {
            // no-op
        }

        private static String decode(String raw) {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        }
    }
}
