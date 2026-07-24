package com.am.market.client.config;

import com.am.portfolio.client.market.api.IndicesApi;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;
import com.am.portfolio.client.market.api.MarketDataApi;
import com.am.portfolio.client.market.api.SecurityExplorerApi;
import com.am.portfolio.client.market.invoker.ApiClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpRequest;
import java.util.function.Consumer;

@Configuration
public class MarketDataClientConfig {

    @Bean
    @ConditionalOnProperty(name = "am.services.market-data-url", matchIfMissing = true)
    public ApiClient marketDataApiClient(@Value("${am.services.market-data-url:http://localhost:8050}") String marketDataUrl,
                                         ObjectProvider<Consumer<HttpRequest.Builder>> authInterceptorProvider) {
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(marketDataUrl);

        com.fasterxml.jackson.databind.ObjectMapper mapper = ApiClient.createDefaultObjectMapper();

        com.fasterxml.jackson.databind.module.SimpleModule mixinModule =
                new com.fasterxml.jackson.databind.module.SimpleModule();
        mixinModule.setMixInAnnotation(
                com.am.portfolio.client.market.model.OHLCVTPoint.class, OHLCVTPointMixin.class);
        mapper.registerModule(mixinModule);

        apiClient.setObjectMapper(mapper);

        // Compose all interceptors (auth + trace, etc.) — getIfAvailable() fails when >1 bean exists
        authInterceptorProvider.orderedStream()
                .reduce(Consumer::andThen)
                .ifPresent(apiClient::setRequestInterceptor);

        return apiClient;
    }

    public abstract static class OHLCVTPointMixin {
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FlexibleOffsetDateTimeDeserializer.class)
        @com.fasterxml.jackson.annotation.JsonProperty("time")
        private java.time.OffsetDateTime time;
    }

    public static class FlexibleOffsetDateTimeDeserializer
            extends com.fasterxml.jackson.databind.JsonDeserializer<java.time.OffsetDateTime> {
        @Override
        public java.time.OffsetDateTime deserialize(com.fasterxml.jackson.core.JsonParser p,
                com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
            String value = p.getText();
            if (value == null) {
                return null;
            }
            try {
                return java.time.OffsetDateTime.parse(value);
            } catch (java.time.format.DateTimeParseException e) {
                try {
                    return java.time.LocalDateTime.parse(value).atZone(java.time.ZoneId.systemDefault())
                            .toOffsetDateTime();
                } catch (java.time.format.DateTimeParseException ex) {
                    throw e;
                }
            }
        }
    }

    @Bean
    public MarketDataApi marketDataApi(@Qualifier("marketDataApiClient") ApiClient marketDataApiClient) {
        return new MarketDataApi(marketDataApiClient);
    }

    @Bean
    public SecurityExplorerApi securityExplorerApi(@Qualifier("marketDataApiClient") ApiClient marketDataApiClient) {
        return new SecurityExplorerApi(marketDataApiClient);
    }

    @Bean
    public MarketAnalyticsApi marketAnalyticsApi(@Qualifier("marketDataApiClient") ApiClient marketDataApiClient) {
        return new MarketAnalyticsApi(marketDataApiClient);
    }

    @Bean
    public IndicesApi indicesApi(@Qualifier("marketDataApiClient") ApiClient marketDataApiClient) {
        return new IndicesApi(marketDataApiClient);
    }

    // Qualifier required when another ApiClient bean exists in the consuming app.
}
