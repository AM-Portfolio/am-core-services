package com.am.trade.client.service;

import am.trade.sdk.AmTradeSdk;
import com.am.domain.trade.TradePortfolio;
import com.am.domain.trade.TradeTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradeClientService {

    private final AmTradeSdk tradeSdk;

    public List<TradePortfolio> getPortfolios(String userId) {
        try {
            Map<String, Object> response = tradeSdk.getPortfolioClient().getAllPortfolios(0, 100);
            return mapPortfolios(response);
        } catch (Exception e) {
            log.error("Failed to fetch trade portfolios for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    public List<TradeTransaction> getRecentTrades(String userId) {
        return getRecentTrades(userId, 0, 20);
    }

    public List<TradeTransaction> getRecentTrades(String userId, int page, int size) {
        try {
            Map<String, Object> response = tradeSdk.getTradeClient().getTradesByFreeTab(page, size);
            return mapTrades(response, userId);
        } catch (Exception e) {
            log.error("Failed to fetch recent trades for user: {}", userId, e);
            return Collections.emptyList();
        }
    }

    public List<TradeTransaction> getTradesBySymbol(String userId, String symbol, int page, int size) {
        try {
            Map<String, Object> response = tradeSdk.getTradeClient()
                    .getTradesByFreeTabAndSymbol(symbol, page, size);
            return mapTrades(response, userId);
        } catch (Exception e) {
            log.error("Failed to fetch trades for symbol {}: {}", symbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> filterTrades(Map<String, Object> filters) {
        try {
            return tradeSdk.getTradeClient().filterTrades(filters);
        } catch (Exception e) {
            log.error("Failed to filter trades: {}", e.getMessage());
            return Map.of("error", e.getMessage() != null ? e.getMessage() : "filter failed");
        }
    }

    public Map<String, Object> getTradeMetrics(String portfolioId) {
        try {
            return tradeSdk.getAnalyticsClient().getTradeMetrics(portfolioId);
        } catch (Exception e) {
            log.error("Failed to fetch trade metrics for {}: {}", portfolioId, e.getMessage());
            return Map.of("error", e.getMessage() != null ? e.getMessage() : "metrics failed");
        }
    }

    public Map<String, Object> getTradePortfolioSummary(String portfolioId) {
        try {
            return tradeSdk.getPortfolioClient().getPortfolioSummary(portfolioId);
        } catch (Exception e) {
            log.error("Failed to fetch trade portfolio summary for {}: {}", portfolioId, e.getMessage());
            return Map.of("error", e.getMessage() != null ? e.getMessage() : "summary failed");
        }
    }

    @SuppressWarnings("unchecked")
    private List<TradeTransaction> mapTrades(Map<String, Object> response, String userId) {
        if (response == null) {
            return Collections.emptyList();
        }
        Object content = response.get("content");
        if (content == null) {
            content = response.get("trades");
        }
        if (content == null) {
            content = response.get("data");
        }
        if (!(content instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<TradeTransaction> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> raw) {
                Map<String, Object> m = (Map<String, Object>) raw;
                result.add(TradeTransaction.builder()
                        .tradeId(asString(m.get("id"), m.get("tradeId"), m.get("trade_id")))
                        .portfolioId(asString(m.get("portfolioId"), m.get("portfolio_id")))
                        .userId(userId)
                        .symbol(asString(m.get("symbol")))
                        .type(asString(m.get("tradeType"), m.get("trade_type"), m.get("type")))
                        .quantity(asBigDecimal(m.get("quantity")))
                        .price(asBigDecimal(m.get("entryPrice"), m.get("entry_price"), m.get("price")))
                        .date(asDate(m.get("entryDate"), m.get("entry_date"), m.get("date")))
                        .status(asString(m.get("status")))
                        .pnl(asBigDecimal(m.get("pnl")))
                        .pnlPercentage(asDouble(m.get("pnlPercentage"), m.get("pnl_percentage")))
                        .build());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<TradePortfolio> mapPortfolios(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return Collections.emptyList();
        }
        Object content = response.get("content");
        if (content == null) {
            content = response.get("portfolios");
        }
        if (!(content instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<TradePortfolio> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> raw) {
                Map<String, Object> m = (Map<String, Object>) raw;
                result.add(TradePortfolio.builder()
                        .id(asString(m.get("id"), m.get("portfolioId")))
                        .name(asString(m.get("name")))
                        .userId(asString(m.get("userId")))
                        .type(asString(m.get("type")))
                        .externalPortfolioId(asString(m.get("externalPortfolioId")))
                        .totalValue(asBigDecimal(m.get("totalValue")))
                        .totalInvested(asBigDecimal(m.get("totalInvested")))
                        .currentPnl(asBigDecimal(m.get("currentPnl"), m.get("pnl")))
                        .pnlPercentage(asDouble(m.get("pnlPercentage")))
                        .build());
            }
        }
        return result;
    }

    private static String asString(Object... candidates) {
        for (Object c : candidates) {
            if (c != null) {
                return String.valueOf(c);
            }
        }
        return null;
    }

    private static BigDecimal asBigDecimal(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) {
                continue;
            }
            try {
                return new BigDecimal(String.valueOf(c));
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private static Double asDouble(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) {
                continue;
            }
            try {
                return Double.valueOf(String.valueOf(c));
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }

    private static LocalDateTime asDate(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) {
                continue;
            }
            try {
                String s = String.valueOf(c);
                if (s.length() >= 19) {
                    return LocalDateTime.parse(s.substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
                return LocalDateTime.parse(s);
            } catch (Exception ignored) {
                // try next
            }
        }
        return null;
    }
}
