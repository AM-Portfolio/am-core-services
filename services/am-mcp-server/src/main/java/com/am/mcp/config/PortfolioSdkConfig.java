package com.am.mcp.config;

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
                                        ObjectProvider<Consumer<HttpRequest.Builder>> traceInterceptorProvider) {
        ApiClient client = new ApiClient();

        client.updateBaseUri(props.getServices().getPortfolioUrl());

        client.setConnectTimeout(Duration.ofMillis(props.getTimeouts().getConnectMs()));
        client.setReadTimeout(Duration.ofMillis(props.getTimeouts().getReadMs()));

        Consumer<HttpRequest.Builder> traceInterceptor = traceInterceptorProvider.getIfAvailable();
        if (traceInterceptor != null) {
            client.setRequestInterceptor(traceInterceptor);
        }

        return client;
    }

    @Bean
    public com.am.portfolio.client.market.invoker.ApiClient portfolioMarketApiClient(AmMcpProperties props,
                                                                                      ObjectProvider<Consumer<HttpRequest.Builder>> traceInterceptorProvider) {
        com.am.portfolio.client.market.invoker.ApiClient client = new com.am.portfolio.client.market.invoker.ApiClient();
        client.updateBaseUri(props.getServices().getPortfolioUrl());
        client.setConnectTimeout(Duration.ofMillis(props.getTimeouts().getConnectMs()));
        client.setReadTimeout(Duration.ofMillis(props.getTimeouts().getReadMs()));

        Consumer<HttpRequest.Builder> traceInterceptor = traceInterceptorProvider.getIfAvailable();
        if (traceInterceptor != null) {
            client.setRequestInterceptor(traceInterceptor);
        }

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
}
