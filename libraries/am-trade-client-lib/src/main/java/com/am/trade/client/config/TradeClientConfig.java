package com.am.trade.client.config;

import am.trade.sdk.AmTradeSdk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TradeClientConfig {

    @Bean
    public AmTradeSdk amTradeSdk(@Value("${am.services.trade.url:http://localhost:8040}") String tradeUrl) {
        return AmTradeSdk.builder()
                .apiUrl(tradeUrl)
                .build();
    }
}
