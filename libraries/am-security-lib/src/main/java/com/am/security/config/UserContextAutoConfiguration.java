package com.am.security.config;

import com.am.security.context.UserContextFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
public class UserContextAutoConfiguration {
    // Dummy comment to test CI/CD wait/dependencies logic
    @Bean
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }
}
