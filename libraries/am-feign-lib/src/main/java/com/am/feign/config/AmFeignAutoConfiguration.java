package com.am.feign.config;

import com.am.feign.interceptor.AuthRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmFeignAutoConfiguration {

    @Bean
    public RequestInterceptor commonAuthRequestInterceptor() {
        return new AuthRequestInterceptor();
    }

    @Bean
    public feign.Logger.Level feignLoggerLevel() {
        return feign.Logger.Level.FULL;
    }

    @Bean
    public feign.Logger feignLogger() {
        return new com.am.feign.logger.CurlLogger();
    }
}
