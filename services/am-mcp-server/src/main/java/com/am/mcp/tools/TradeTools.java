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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Trade domain MCP tools.
 * Uses TradeClientService (AmTradeSdk) and AnalysisRepository for P&L
 * aggregation.
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
            Get the most recent portfolio transactions: buys, sells, sorted newest first.
            Use this when asked: "What did I buy recently?", "Show my last trades",
            "What transactions did I make this week?", "Show recent activity."
            limit: number of items to return (default: 20).
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "activityFallback")
    public String getRecentActivity(
            @ToolParam(description = "User ID.") String userId,
            @ToolParam(description = "Number of recent items (default 20, max 100).") Integer limit) {
        String uid = resolve(userId);
        int count = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
        List<TradeTransaction> trades = tradeClientService.getRecentTrades(uid)
                .stream().limit(count).collect(Collectors.toList());
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("activities", trades);
        result.put("count", trades.size());
        return response.toJson(result);
    }

    public String activityFallback(String u, Integer l, Exception e) {
        return response.unavailable("am-trade (recent activity)");
    }

    @Tool(name = "get_trade_history", description = """
            Get the full transaction history for a specific stock.
            Use this when asked: "Show all my RELIANCE trades",
            "When did I buy HDFC Bank?", "How many times have I traded TCS?"
            """)
    @CircuitBreaker(name = "am-trade", fallbackMethod = "historyFallback")
    public String getTradeHistory(
            @ToolParam(description = "User ID.") String userId,
            @ToolParam(description = "Stock symbol or partial name (e.g. 'RELIANCE', 'HDFC', 'TCS').") String symbol) {
        String uid = resolve(userId);
        List<TradeTransaction> filtered = tradeClientService.getRecentTrades(uid).stream()
                .filter(t -> t.getSymbol() != null
                        && t.getSymbol().toLowerCase().contains(symbol.toLowerCase()))
                .collect(Collectors.toList());
        if (filtered.isEmpty())
            return "{\"message\":\"No trade history found for '" + symbol + "'\"}";
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("trades", filtered);
        result.put("count", filtered.size());
        return response.toJson(result);
    }

    public String historyFallback(String u, String s, Exception e) {
        return response.unavailable("am-trade (trade history)");
    }

    @Tool(name = "get_unrealised_pnl", description = """
            Get current unrealised P&L across all holdings: total invested,
            current market value, gain/loss amount and percentage.
            Use this when asked: "What is my unrealised profit?",
            "How much have I made overall?", "Show my portfolio P&L summary."
            """)
    @CircuitBreaker(name = "am-analysis", fallbackMethod = "pnlFallback")
    public String getUnrealisedPnl(
            @ToolParam(description = "User ID.") String userId) {
        String uid = resolve(userId);
        List<AnalysisEntity> entities = analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.HOLDING);

        double invested = 0, current = 0;
        for (AnalysisEntity e : entities) {
            if (e.getHoldings() == null)
                continue;
            for (AnalysisHolding h : e.getHoldings()) {
                var inv = h.getInvestment();
                if (inv != null) {
                    if (inv.getInvestmentValue() != null)
                        invested += inv.getInvestmentValue();
                    if (inv.getCurrentValue() != null)
                        current += inv.getCurrentValue();
                }
            }
        }
        double pnl = current - invested;
        double pnlPct = invested > 0 ? Math.round((pnl / invested) * 10000.0) / 100.0 : 0.0;

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalInvested", Math.round(invested * 100.0) / 100.0);
        result.put("currentValue", Math.round(current * 100.0) / 100.0);
        result.put("unrealisedPnL", Math.round(pnl * 100.0) / 100.0);
        result.put("unrealisedPnLPercent", pnlPct);
        result.put("holdingsCount", entities.size());
        return response.toJson(result);
    }

    public String pnlFallback(String u, Exception e) {
        return response.unavailable("am-analysis (P&L)");
    }

    private String resolve(String userId) {
        return (userId != null && !userId.isBlank()) ? userId : props.getDefaults().getUserId();
    }
}
