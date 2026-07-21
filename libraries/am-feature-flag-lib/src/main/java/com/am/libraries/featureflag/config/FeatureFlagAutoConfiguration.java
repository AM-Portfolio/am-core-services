package com.am.libraries.featureflag.config;

import com.am.libraries.featureflag.aop.FeatureFlagAspect;
import com.am.libraries.featureflag.service.GrowthBookService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Auto-Configuration for Feature Flag library.
 */
@Configuration
@EnableConfigurationProperties(FeatureFlagProperties.class)
@ConditionalOnProperty(prefix = "am.feature-flag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeatureFlagAutoConfiguration {

    @Bean
    public GrowthBookService growthBookService(FeatureFlagProperties properties) {
        return new GrowthBookService(properties);
    }

    @Bean
    public FeatureFlagAspect featureFlagAspect(GrowthBookService growthBookService) {
        return new FeatureFlagAspect(growthBookService);
    }
}
