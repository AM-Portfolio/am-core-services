package com.am.gateway.config;

import com.am.kafka.service.InterestRegistryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class InterestRegistryConfig {

    @Bean
    public InterestRegistryService interestRegistryService(StringRedisTemplate redisTemplate) {
        return new InterestRegistryService(redisTemplate);
    }
}
