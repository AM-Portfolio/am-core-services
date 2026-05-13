package com.am.trade.client.service;

import am.trade.sdk.AmTradeSdk;
import am.trade.sdk.dto.TradeDTO;
import com.am.domain.trade.TradePortfolio;
import com.am.domain.trade.TradeTransaction;
import com.am.domain.trade.TradeHolding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeClientService {

    private final AmTradeSdk tradeSdk;

    /**
     * Get all portfolios for a user from Trade SDK
     */
    public List<TradePortfolio> getPortfolios(String userId) {
        try {
            log.debug("Fetching portfolios for user: {}", userId);
            List<Map<String, Object>> portfolioData = tradeSdk.getPortfolioClient().getPortfoliosByOwner(userId);
            
            if (portfolioData == null || portfolioData.isEmpty()) {
                return Collections.emptyList();
            }

            return portfolioData.stream()
                .map(this::mapToTradePortfolio)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch trade portfolios for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    private TradePortfolio mapToTradePortfolio(Map<String, Object> data) {
        return TradePortfolio.builder()
            .id((String) data.get("portfolioId"))
            .name((String) data.get("name"))
            .type((String) data.get("type"))
            .ownerId((String) data.get("ownerId"))
            .build();
    }

    /**
     * Get recent trades for a user
     */
    public List<TradeTransaction> getRecentTrades(String userId) {
        try {
            // leveraging the sdk's trade client
            // Map<String, Object> trades = tradeSdk.getTradeClient().getTradesByFreeTab(0, 10);
            // Transform to TradeTransaction
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to fetch recent trades for user: {}", userId, e);
            return Collections.emptyList();
        }
    }
}
