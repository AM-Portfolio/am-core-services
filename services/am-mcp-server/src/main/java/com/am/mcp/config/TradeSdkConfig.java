package com.am.mcp.config;

import am.trade.sdk.AmTradeSdk;
import com.am.mcp.auth.AuthTokenProvider;
import com.am.trade.client.auth.TradeAuthTokenSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wires AmTradeSdk with blank apiKey at init; outbound trade calls use the live
 * per-request user JWT from {@link AuthTokenProvider} via {@link TradeAuthTokenSupplier}.
 */
@Configuration
public class TradeSdkConfig {

    @Bean
    @Primary
    public AmTradeSdk amTradeSdk(
            @Value("${am.services.trade-url:http://localhost:8040}") String tradeUrl) {
        return AmTradeSdk.builder()
                .apiUrl(tradeUrl)
                .apiKey("")
                .build();
    }

    @Bean
    public TradeAuthTokenSupplier tradeAuthTokenSupplier(AuthTokenProvider authTokenProvider) {
        return authTokenProvider::getToken;
    }
}
