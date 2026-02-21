package com.am.analysis.config;

import com.am.analysis.service.orchestrator.DemandDrivenOrchestrator;
import com.am.kafka.service.InterestRegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Wires the Demand-Driven Orchestrator and its Redis-based Interest Registry dependency.
 *
 * The Orchestrator is a separate @Service group from AnalysisAggregator, allowing it
 * to be extracted into its own module in the future without refactoring.
 */
@Configuration
@Slf4j
public class OrchestratorConfig {

    @Bean
    public InterestRegistryService interestRegistryService(StringRedisTemplate redisTemplate) {
        log.info("[OrchestratorConfig] Wiring InterestRegistryService with Redis");
        return new InterestRegistryService(redisTemplate);
    }

    @Bean
    public DemandDrivenOrchestrator demandDrivenOrchestrator(
            InterestRegistryService interestRegistryService,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        log.info("[OrchestratorConfig] Wiring DemandDrivenOrchestrator");
        return new DemandDrivenOrchestrator(interestRegistryService, kafkaTemplate, objectMapper);
    }
}
