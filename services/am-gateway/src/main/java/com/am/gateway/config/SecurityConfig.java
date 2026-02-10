package com.am.gateway.config;

import com.am.gateway.security.JwtUtilMock;
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

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtilMock jwtUtil;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Defensive null check
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authHeader = accessor.getNativeHeader("Authorization");
                    if (authHeader != null && !authHeader.isEmpty()) {
                        String token = authHeader.get(0);
                        // Strip 'Bearer ' prefix if present for cleaner validation
                        if (token.startsWith("Bearer ")) {
                            token = token.substring(7);
                        }

                        if (jwtUtil.validateToken(token)) {
                            // Valid
                            String userId = jwtUtil.getUserId(token);
                            if (userId != null) {
                                // Create a custom Principal without Spring Security dependency
                                java.security.Principal userPrincipal = new java.security.Principal() {
                                    @Override
                                    public String getName() {
                                        return userId;
                                    }
                                };
                                accessor.setUser(userPrincipal);
                                log.info("Client Connected: {} | User: {}", accessor.getSessionId(), userId);
                            } else {
                                log.warn("Token valid but no userId found. Session: {}", accessor.getSessionId());
                            }
                            return message;
                        }
                    }
                    log.warn("Unauthorized WebSocket Connection Attempt: Session {}", accessor.getSessionId());
                    throw new IllegalArgumentException("Unauthorized");
                }
                return message;
            }
        });
    }
}
