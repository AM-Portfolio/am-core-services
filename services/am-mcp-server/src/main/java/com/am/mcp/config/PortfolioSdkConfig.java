package com.am.mcp.config;

import com.am.portfolio.client.api.PortfolioAnalyticsApi;
import com.am.portfolio.client.api.PortfolioManagementApi;
import com.am.portfolio.client.invoker.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class PortfolioSdkConfig {

    @Bean
    public ApiClient portfolioApiClient(AmMcpProperties props) {
        // We can use the default HttpClient or configure one.
        // The generated SDK uses Java 11's native HttpClient.
        ApiClient client = new ApiClient();

        // Base URI for portfolio service (Port 8060 per Global Rules)
        client.updateBaseUri(props.getServices().getPortfolioUrl());

        // Use timeouts from properties
        client.setConnectTimeout(Duration.ofMillis(props.getTimeouts().getConnectMs()));
        client.setReadTimeout(Duration.ofMillis(props.getTimeouts().getReadMs()));

        return client;
    }

    @Bean
    public com.am.portfolio.client.market.invoker.ApiClient portfolioMarketApiClient(AmMcpProperties props) {
        com.am.portfolio.client.market.invoker.ApiClient client = new com.am.portfolio.client.market.invoker.ApiClient();
        client.updateBaseUri(props.getServices().getPortfolioUrl());
        client.setConnectTimeout(Duration.ofMillis(props.getTimeouts().getConnectMs()));
        client.setReadTimeout(Duration.ofMillis(props.getTimeouts().getReadMs()));
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
