package com.am.mcp.config;

import am.trade.sdk.AmTradeSdk;
import com.am.mcp.auth.AuthTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Trade SDK with live JWT from AuthTokenProvider (overrides library default bean).
 */
@Configuration
public class TradeSdkConfig {

    @Bean
    @Primary
    public AmTradeSdk amTradeSdk(
            @Value("${am.services.trade-url:http://localhost:8040}") String tradeUrl,
            AuthTokenProvider authTokenProvider) {
        String token = authTokenProvider.getToken();
        return AmTradeSdk.builder()
                .apiUrl(tradeUrl)
                .apiKey(token != null ? token : "")
                .build();
    }
}
