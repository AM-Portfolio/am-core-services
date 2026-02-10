package com.am.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@EnableScheduling
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS endpoint (fallback for browsers that don't support WebSocket)
        registry.addEndpoint("/ws-gateway")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket endpoint (for clients with SockJS compatibility issues)
        registry.addEndpoint("/ws-gateway-raw")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * TaskScheduler bean for managing periodic portfolio calculation triggers.
     * Used by PortfolioSubscriptionManager to schedule calculations per subscribed
     * user.
     */
    @Bean
    public TaskScheduler portfolioTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // Support up to 10 concurrent scheduled tasks
        scheduler.setThreadNamePrefix("portfolio-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
