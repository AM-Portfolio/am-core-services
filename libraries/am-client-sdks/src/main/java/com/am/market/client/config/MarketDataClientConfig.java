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

        com.fasterxml.jackson.databind.ObjectMapper mapper = ApiClient.createDefaultObjectMapper();

        com.fasterxml.jackson.databind.module.SimpleModule mixinModule = new com.fasterxml.jackson.databind.module.SimpleModule();
        mixinModule.setMixInAnnotation(com.am.portfolio.client.market.model.OHLCVTPoint.class, OHLCVTPointMixin.class);
        mapper.registerModule(mixinModule);

        apiClient.setObjectMapper(mapper);

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
                // Fallback: Try parsing as LocalDateTime and convert to OffsetDateTime
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
    public MarketDataApi marketDataApi(ApiClient marketDataApiClient) {
        return new MarketDataApi(marketDataApiClient);
    }

    @Bean
    public SecurityExplorerApi securityExplorerApi(ApiClient marketDataApiClient) {
        return new SecurityExplorerApi(marketDataApiClient);
    }
}
