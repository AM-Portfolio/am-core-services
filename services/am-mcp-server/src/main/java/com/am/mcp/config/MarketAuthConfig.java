package com.am.mcp.config;

import com.am.mcp.auth.AuthTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

/**
 * Shared HTTP request interceptor so market-client-lib ApiClient gets Bearer auth.
 */
@Configuration
public class MarketAuthConfig {

    @Bean
    public Consumer<HttpRequest.Builder> marketAuthInterceptor(AuthTokenProvider authTokenProvider) {
        return builder -> {
            String token = authTokenProvider.getToken();
            if (token != null && !token.isBlank()) {
                builder.setHeader("Authorization", "Bearer " + token);
            }
        };
    }
}
