package com.am.analysis.config;

import com.am.analysis.service.orchestrator.DemandDrivenOrchestrator;
import com.am.kafka.service.InterestRegistryService;
import com.am.kafka.service.PreviousCloseRedisService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.trace.TracingHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wires the Demand-Driven Orchestrator and its Redis-based Interest Registry dependency.
 */
@Configuration
@Slf4j
public class OrchestratorConfig {

    @Bean
    public InterestRegistryService interestRegistryService(StringRedisTemplate redisTemplate) {
        return new InterestRegistryService(redisTemplate);
    }

    @Bean
    public PreviousCloseRedisService previousCloseRedisService(StringRedisTemplate redisTemplate) {
        return new PreviousCloseRedisService(redisTemplate);
    }

    @Bean
    public DemandDrivenOrchestrator demandDrivenOrchestrator(
            InterestRegistryService interestRegistryService,
            com.am.analysis.service.DashboardAnalysisService dashboardAnalysisService,
            ObjectMapper objectMapper,
            FlowLogger flowLogger,
            TracingHelper tracingHelper,
            com.am.analysis.service.PortfolioStreamingService portfolioStreamingService,
            com.am.analysis.config.PortfolioStreamingProperties portfolioStreamingProperties,
            PreviousCloseRedisService previousCloseRedisService,
            com.am.analysis.service.bootstrap.PortfolioBootstrapTrigger portfolioBootstrapTrigger,
            com.am.analysis.service.bootstrap.TriggerCalculationPublisher triggerCalculationPublisher) {
        return new DemandDrivenOrchestrator(interestRegistryService, objectMapper,
                portfolioBootstrapTrigger, triggerCalculationPublisher,
                flowLogger, tracingHelper, dashboardAnalysisService,
                portfolioStreamingService, portfolioStreamingProperties, previousCloseRedisService);
    }
}
