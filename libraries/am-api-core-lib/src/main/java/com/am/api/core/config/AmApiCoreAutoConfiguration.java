package com.am.api.core.config;

import com.am.api.core.advice.GlobalExceptionHandler;
import com.am.api.core.advice.GlobalResponseWrapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AmApiCoreAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public GlobalResponseWrapper globalResponseWrapper() {
        return new GlobalResponseWrapper();
    }
}
