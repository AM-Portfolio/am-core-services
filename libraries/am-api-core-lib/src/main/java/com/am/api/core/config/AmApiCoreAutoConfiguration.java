package com.am.api.core.config;

import com.am.api.core.advice.GlobalExceptionHandler;
import com.am.api.core.advice.GlobalResponseWrapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AmApiCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "globalExceptionHandler")
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "am.api.core.exception-handler", name = "enabled", havingValue = "true", matchIfMissing = false)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(name = "globalResponseWrapper")
    public GlobalResponseWrapper globalResponseWrapper() {
        return new GlobalResponseWrapper();
    }
}
