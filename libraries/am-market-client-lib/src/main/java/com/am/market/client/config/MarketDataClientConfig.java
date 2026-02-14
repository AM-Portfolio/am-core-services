package com.am.market.client.config;

import com.am.portfolio.client.market.api.MarketDataApi;
import com.am.portfolio.client.market.api.SecurityExplorerApi;
import com.am.portfolio.client.market.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketDataClientConfig {

    @Bean
    @ConditionalOnProperty(name = "am.services.market-data.url", matchIfMissing = false)
    public ApiClient marketDataApiClient(@Value("${am.services.market-data.url}") String marketDataUrl) {
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(marketDataUrl);
        return apiClient;
    }

    @Bean
    public MarketDataApi marketDataApi(ApiClient marketDataApiClient) {
        return new MarketDataApi(marketDataApiClient);
    }

    @Bean
    public SecurityExplorerApi securityExplorerApi(ApiClient marketDataApiClient) {
        return new SecurityExplorerApi(marketDataApiClient);
    }
}
