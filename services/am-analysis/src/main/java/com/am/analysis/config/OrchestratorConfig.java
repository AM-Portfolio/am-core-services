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
import org.springframework.kafka.core.KafkaTemplate;

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
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            FlowLogger flowLogger,
            TracingHelper tracingHelper,
            com.am.analysis.service.PortfolioStreamingService portfolioStreamingService,
            com.am.analysis.config.PortfolioStreamingProperties portfolioStreamingProperties,
            PreviousCloseRedisService previousCloseRedisService) {
        return new DemandDrivenOrchestrator(interestRegistryService, kafkaTemplate, objectMapper,
                flowLogger, tracingHelper, dashboardAnalysisService,
                portfolioStreamingService, portfolioStreamingProperties, previousCloseRedisService);
    }
}
