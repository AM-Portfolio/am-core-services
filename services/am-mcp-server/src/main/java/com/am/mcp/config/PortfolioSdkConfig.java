package com.am.mcp.config;

import com.am.mcp.auth.AuthTokenProvider;
import com.am.mcp.jackson.PortfolioObjectMappers;
import com.am.observability.http.TraceContextSdkInterceptor;
import com.am.portfolio.client.api.PortfolioAnalyticsApi;
import com.am.portfolio.client.api.PortfolioManagementApi;
import com.am.portfolio.client.invoker.ApiClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Consumer;

@Configuration
public class PortfolioSdkConfig {

    @Bean
    public ApiClient portfolioApiClient(AmMcpProperties props,
                                        AuthTokenProvider authTokenProvider,
                                        ObjectProvider<TraceContextSdkInterceptor> traceInterceptorProvider) {
        ApiClient client = new ApiClient();

        client.updateBaseUri(props.getServices().getPortfolioUrl());

        client.setConnectTimeout(Duration.ofMillis(props.getTimeouts().getConnectMs()));
        client.setReadTimeout(Duration.ofMillis(props.getTimeouts().getReadMs()));
        client.setObjectMapper(PortfolioObjectMappers.create());

        client.setRequestInterceptor(composeInterceptor(authTokenProvider, traceInterceptorProvider.getIfAvailable()));

        return client;
    }

    @Bean
    public PortfolioManagementApi portfolioManagementApi(ApiClient apiClient) {
        return new PortfolioManagementApi(apiClient);
    }

    @Bean
    public PortfolioAnalyticsApi portfolioAnalyticsApi(ApiClient apiClient) {
        return new PortfolioAnalyticsApi(apiClient);
    }

    private static Consumer<HttpRequest.Builder> composeInterceptor(
            AuthTokenProvider authTokenProvider,
            Consumer<HttpRequest.Builder> traceInterceptor) {
        return builder -> {
            String token = authTokenProvider.getToken();
            if (token != null && !token.isBlank()) {
                builder.setHeader("Authorization", "Bearer " + token);
            }
            if (traceInterceptor != null) {
                traceInterceptor.accept(builder);
            }
        };
    }
}
