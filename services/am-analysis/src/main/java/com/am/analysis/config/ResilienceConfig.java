package com.am.analysis.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for am-analysis cross-service calls.
 *
 * Two circuit breakers are defined:
 *   - portfolioServiceCB  → protects calls to am-portfolio
 *   - tradeServiceCB      → protects calls to am-trade
 *
 * Fallback strategy:
 *   - portfolio down → return Redis cached snapshot with isComplete=false
 *   - trade down     → return live portfolio data only with isComplete=false
 */
@Configuration
public class ResilienceConfig {

    /**
     * Shared circuit breaker config for all downstream services.
     * - Opens after 50% failure rate in a 10-call sliding window.
     * - Stays open for 30 seconds before attempting half-open.
     * - Timeout: 3 seconds per call.
     */
    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(70)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }

    @Bean
    public TimeLimiterConfig defaultTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .build();
    }
}
