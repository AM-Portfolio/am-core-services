package com.am.mcp.tools;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.domain.trade.TradeTransaction;
import com.am.mcp.config.AmMcpProperties;
import com.am.mcp.util.ResponseHelper;
import com.am.trade.client.service.TradeClientService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Trade domain MCP tools — AmTradeSdk via TradeClientService.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.trade", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TradeTools {

    private final TradeClientService tradeClientService;
    private final AnalysisRepository analysisRepository;
    private final AmMcpProperties props;
    private final ResponseHelper response;

    @Tool(name = "get_recent_activity", description = """
            [trade] Get the most recent portfolio transactions: buys, sells, sorted newest first.
            Use this when asked: "What did I buy recently?", "Show my last trades",
            "What transactions did I make this week?", "Show recent activity."
            limit: number of items to return (default: 20).
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "activityFallback")
    public String getRecentActivity(
            @ToolParam(description = "User ID.") String userId,
            @ToolParam(description = "Number of recent items (default 20, max 100).") Integer limit) {
        try {
            String uid = resolve(userId);
            int count = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
            List<TradeTransaction> trades = tradeClientService.getRecentTrades(uid, 0, count);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("activities", trades);
            result.put("count", trades.size());
            return response.toJson(result);
        } catch (Exception e) {
            return response.errorJson("get_recent_activity", e);
        }
    }

    public String activityFallback(String u, Integer l, Exception e) {
        return response.unavailable("am-trade (recent activity)");
    }

    @Tool(name = "get_trade_history", description = """
            [trade] Get the full transaction history for a specific stock.
            Use this when asked: "Show all my RELIANCE trades",
            "When did I buy HDFC Bank?", "How many times have I traded TCS?"
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "historyFallback")
    public String getTradeHistory(
            @ToolParam(description = "User ID.") String userId,
            @ToolParam(description = "Stock symbol or partial name (e.g. 'RELIANCE', 'HDFC', 'TCS').") String symbol) {
        try {
            String uid = resolve(userId);
            List<TradeTransaction> filtered = tradeClientService.getTradesBySymbol(uid, symbol, 0, 50);
            if (filtered.isEmpty()) {
                // Fallback: recent trades filtered client-side
                filtered = tradeClientService.getRecentTrades(uid, 0, 100).stream()
                        .filter(t -> t.getSymbol() != null
                                && t.getSymbol().toLowerCase().contains(symbol.toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (filtered.isEmpty()) {
                return response.failure("get_trade_history", "NOT_FOUND",
                        "No trade history found for '" + symbol + "'", false, null);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("trades", filtered);
            result.put("count", filtered.size());
            return response.toJson(result);
        } catch (Exception e) {
            return response.errorJson("get_trade_history", e);
        }
    }

    public String historyFallback(String u, String s, Exception e) {
        return response.unavailable("am-trade (trade history)");
    }

    @Tool(name = "get_unrealised_pnl", description = """
            [trade] Get current unrealised P&L across all holdings: total invested,
            current market value, gain/loss amount and percentage.
            Use this when asked: "What is my unrealised profit?",
            "How much have I made overall?", "Show my portfolio P&L summary."
            """)
    @CircuitBreaker(name = "am-analysis", fallbackMethod = "pnlFallback")
    public String getUnrealisedPnl(
            @ToolParam(description = "User ID.") String userId) {
        try {
            String uid = resolve(userId);
            List<AnalysisEntity> entities = analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.HOLDING);

            double invested = 0, current = 0;
            for (AnalysisEntity e : entities) {
                if (e.getHoldings() == null) {
                    continue;
                }
                for (AnalysisHolding h : e.getHoldings()) {
                    var inv = h.getInvestment();
                    if (inv != null) {
                        if (inv.getInvestmentValue() != null) {
                            invested += inv.getInvestmentValue();
                        }
                        if (inv.getCurrentValue() != null) {
                            current += inv.getCurrentValue();
                        }
                    }
                }
            }
            double pnl = current - invested;
            double pnlPct = invested > 0 ? Math.round((pnl / invested) * 10000.0) / 100.0 : 0.0;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalInvested", Math.round(invested * 100.0) / 100.0);
            result.put("currentValue", Math.round(current * 100.0) / 100.0);
            result.put("unrealisedPnL", Math.round(pnl * 100.0) / 100.0);
            result.put("unrealisedPnLPercent", pnlPct);
            result.put("holdingsCount", entities.size());
            return response.toJson(result);
        } catch (Exception e) {
            return response.errorJson("get_unrealised_pnl", e);
        }
    }

    public String pnlFallback(String u, Exception e) {
        return response.unavailable("am-analysis (P&L)");
    }

    @Tool(name = "filter_trades", description = """
            [trade] Filter trades by symbol, status, and/or portfolioId.
            Use this when asked: "Show open RELIANCE trades", "Filter trades by status CLOSED".
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "filterFallback")
    public String filterTrades(
            @ToolParam(description = "Optional stock symbol.") String symbol,
            @ToolParam(description = "Optional status (e.g. OPEN, CLOSED).") String status,
            @ToolParam(description = "Optional portfolio UUID.") String portfolioId) {
        try {
            Map<String, Object> filters = new HashMap<>();
            if (symbol != null && !symbol.isBlank()) {
                filters.put("symbol", symbol);
            }
            if (status != null && !status.isBlank()) {
                filters.put("status", status);
            }
            if (portfolioId != null && !portfolioId.isBlank()) {
                filters.put("portfolioId", portfolioId);
            }
            return response.toJson(tradeClientService.filterTrades(filters));
        } catch (Exception e) {
            return response.errorJson("filter_trades", e);
        }
    }

    public String filterFallback(String s, String st, String p, Exception e) {
        return response.unavailable("am-trade (filter)");
    }

    @Tool(name = "get_trades_by_date_range", description = """
            [trade] Get trades within a date range (YYYY-MM-DD).
            Use this when asked: "Trades last month", "Show trades between Jan and Mar."
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "dateRangeFallback")
    public String getTradesByDateRange(
            @ToolParam(description = "Start date YYYY-MM-DD.") String fromDate,
            @ToolParam(description = "End date YYYY-MM-DD.") String toDate,
            @ToolParam(description = "Optional portfolio UUID.") String portfolioId) {
        try {
            Map<String, Object> filters = new HashMap<>();
            filters.put("fromDate", fromDate);
            filters.put("toDate", toDate);
            if (portfolioId != null && !portfolioId.isBlank()) {
                filters.put("portfolioId", portfolioId);
            }
            return response.toJson(tradeClientService.filterTrades(filters));
        } catch (Exception e) {
            return response.errorJson("get_trades_by_date_range", e);
        }
    }

    public String dateRangeFallback(String f, String t, String p, Exception e) {
        return response.unavailable("am-trade (date range)");
    }

    @Tool(name = "get_trade_metrics", description = """
            [trade] Get trade analytics metrics for a trade portfolio (win rate, PnL stats, etc.).
            Use this when asked: "Trade metrics for my portfolio", "Win rate and average PnL."
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "metricsFallback")
    public String getTradeMetrics(
            @ToolParam(description = "Trade portfolio UUID.") String portfolioId) {
        try {
            return response.toJson(tradeClientService.getTradeMetrics(portfolioId));
        } catch (Exception e) {
            return response.errorJson("get_trade_metrics", e);
        }
    }

    public String metricsFallback(String p, Exception e) {
        return response.unavailable("am-trade (metrics)");
    }

    @Tool(name = "get_trade_portfolio_summaries", description = """
            [trade] Get trade-domain portfolio summary (distinct from portfolio get_portfolio_summary).
            Use this when asked: "Trade portfolio summary", "Trading account performance overview."
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "tradeSummaryFallback")
    public String getTradePortfolioSummaries(
            @ToolParam(description = "Trade portfolio UUID.") String portfolioId) {
        try {
            return response.toJson(tradeClientService.getTradePortfolioSummary(portfolioId));
        } catch (Exception e) {
            return response.errorJson("get_trade_portfolio_summaries", e);
        }
    }

    public String tradeSummaryFallback(String p, Exception e) {
        return response.unavailable("am-trade (portfolio summary)");
    }

    private String resolve(String userId) {
        return com.am.mcp.util.UserIdResolver.resolve(userId, props);
    }
}
