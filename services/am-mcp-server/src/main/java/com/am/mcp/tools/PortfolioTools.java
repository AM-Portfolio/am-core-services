package com.am.mcp.tools;

import com.am.mcp.config.AmMcpProperties;
import com.am.mcp.util.ResponseHelper;
import com.am.portfolio.client.api.PortfolioAnalyticsApi;
import com.am.portfolio.client.api.PortfolioManagementApi;
import com.am.portfolio.client.model.PortfolioBasicInfo;
import com.am.portfolio.client.model.PortfolioModelV1;
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
 * Portfolio domain MCP tools.
 * Uses PortfolioManagementApi and PortfolioAnalyticsApi
 * (am-portfolio-client-lib SDK).
 *
 * Each tool returns a slim view of the data to stay within
 * the LLM context limit (am.mcp.max-response-chars in application.yaml).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.portfolio", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PortfolioTools {

    private final PortfolioManagementApi portfolioManagementApi;
    private final PortfolioAnalyticsApi portfolioAnalyticsApi;
    private final AmMcpProperties props;
    private final ResponseHelper response;

    // ── Tool: Portfolio Summary ───────────────────────────────────────────────

    @Tool(name = "get_portfolio_summary", description = """
            Get overall portfolio performance for the user.
            Returns: total invested value, current market value, unrealised P&L (amount + %),
            and day change. Use this when asked:
              "What is my portfolio value?", "How is my portfolio performing?",
              "What are my total returns?", "How much have I made/lost?"
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "portfolioSummaryFallback")
    public String getPortfolioSummary(
            @ToolParam(description = "User ID. Leave blank to use the configured default user.") String userId) {
        try {
            String uid = resolve(userId);
            log.info("[MCP] get_portfolio_summary called for userId: {} (resolved: {})", userId, uid);
            // Fetch all portfolios for the user
            List<PortfolioBasicInfo> portfolios = portfolioManagementApi.getPortfolioBasicDetails(uid);
            log.debug("[MCP] Found {} portfolios for user: {}", portfolios.size(), uid);

            // Fetch detailed model for each to get performance (this might be heavy, but
            // let's follow the pattern)
            List<Map<String, Object>> summary = portfolios.stream().map(p -> {
                try {
                    PortfolioModelV1 model = portfolioManagementApi.getPortfolioById(p.getPortfolioId());

                    double totalInvested = 0.0;
                    double totalGainLoss = 0.0;

                    if (model.getEquityModels() != null) {
                        for (var eq : model.getEquityModels()) {
                            if (eq.getInvestmentValue() != null)
                                totalInvested += eq.getInvestmentValue();
                            if (eq.getProfitLoss() != null)
                                totalGainLoss += eq.getProfitLoss();
                        }
                    }

                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("portfolioId", p.getPortfolioId());
                    m.put("name", p.getPortfolioName());
                    m.put("totalValue", model.getTotalValue() != null ? model.getTotalValue() : 0.0);
                    m.put("totalInvested", totalInvested);
                    m.put("gainLoss", totalGainLoss);
                    m.put("gainLossPct", totalInvested > 0 ? (totalGainLoss / totalInvested) * 100.0 : 0.0);
                    // dayChange is not directly available in standard model
                    m.put("dayChange", 0.0);
                    m.put("dayChangePct", 0.0);
                    m.put("holdingsCount", model.getAssetCount() != null ? model.getAssetCount()
                            : (model.getEquityModels() != null ? model.getEquityModels().size() : 0));
                    return m;
                } catch (Exception e) {
                    log.error("Failed to fetch detail for portfolio {}", p.getPortfolioId(), e);
                    return null;
                }
            })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            return response.toJson(summary);
        } catch (Exception e) {
            log.error("Failed to fetch portfolio summary for user {}", userId, e);
            throw new RuntimeException(e);
        }
    }

    public String portfolioSummaryFallback(String userId, Exception e) {
        return response.unavailable("am-portfolio (portfolio summary)");
    }

    // ── Tool: Holdings List ───────────────────────────────────────────────────

    @Tool(name = "get_holdings", description = """
            Get the complete list of all stocks and ETFs in the user's portfolio.
            Returns for each holding: symbol, name, quantity, average cost, current value,
            total P&L (amount + %), and day change. Use this when asked:
              "What stocks do I own?", "Show me my holdings", "What is in my portfolio?"
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "holdingsFallback")
    public String getHoldings(
            @ToolParam(description = "User ID. Leave blank for default user.") String userId) {
        try {
            String uid = resolve(userId);
            log.info("[MCP] get_holdings called for userId: {} (resolved: {})", userId, uid);
            List<PortfolioBasicInfo> portfolios = portfolioManagementApi.getPortfolioBasicDetails(uid);

            // Aggregate holdings from all portfolios
            List<Map<String, Object>> allHoldings = portfolios.stream().flatMap(p -> {
                try {
                    PortfolioModelV1 model = portfolioManagementApi.getPortfolioById(p.getPortfolioId());
                    if (model.getEquityModels() == null)
                        return java.util.stream.Stream.empty();

                    return model.getEquityModels().stream().map(h -> {
                        Map<String, Object> m = new java.util.LinkedHashMap<>();
                        m.put("symbol", h.getSymbol());
                        m.put("portfolioId", p.getPortfolioId());
                        m.put("currentValue", h.getCurrentValue() != null ? h.getCurrentValue() : 0.0);
                        m.put("gainLossPct", h.getProfitLossPercentage() != null ? h.getProfitLossPercentage() : 0.0);
                        m.put("dayChangePct", 0.0);
                        return m;
                    });
                } catch (Exception e) {
                    return java.util.stream.Stream.empty();
                }
            }).collect(Collectors.toList());

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("holdings", allHoldings);
            result.put("count", allHoldings.size());
            return response.toJson(result);
        } catch (Exception e) {
            log.error("Failed to fetch holdings for user {}", userId, e);
            throw new RuntimeException(e);
        }
    }

    public String holdingsFallback(String userId, Exception e) {
        return response.unavailable("am-portfolio (holdings)");
    }

    // ── Tool: Holding Detail ──────────────────────────────────────────────────

    @Tool(name = "get_holding_detail", description = """
            Get detailed P&L and metrics for a specific stock in the user's portfolio.
            Returns: quantity, average cost, current price, invested amount, current value,
            unrealised P&L, sector, market cap type. Use this when asked:
              "How is my RELIANCE doing?", "What is my profit on HDFC Bank?",
              "Tell me about my TCS holding."
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "holdingDetailFallback")
    public String getHoldingDetail(
            @ToolParam(description = "User ID.") String userId,
            @ToolParam(description = "Stock symbol (e.g. 'RELIANCE', 'HDFC', 'TCS').") String symbol) {
        try {
            String uid = resolve(userId);
            List<PortfolioBasicInfo> portfolios = portfolioManagementApi.getPortfolioBasicDetails(uid);

            for (PortfolioBasicInfo p : portfolios) {
                PortfolioModelV1 model = portfolioManagementApi.getPortfolioById(p.getPortfolioId());
                if (model.getEquityModels() != null) {
                    var match = model.getEquityModels().stream()
                            .filter(h -> h.getSymbol() != null && h.getSymbol().equalsIgnoreCase(symbol))
                            .findFirst();
                    if (match.isPresent()) {
                        return response.toJson(match.get());
                    }
                }
            }
            return "{\"message\":\"No holding found matching '" + symbol + "'\"}";
        } catch (Exception e) {
            log.error("Failed to fetch holding detail for user {} and symbol {}", userId, symbol, e);
            throw new RuntimeException(e);
        }
    }

    public String holdingDetailFallback(String userId, String symbol, Exception e) {
        return response.unavailable("am-portfolio (holding detail)");
    }

    // ── Tool: Portfolio Overviews ─────────────────────────────────────────────

    @Tool(name = "get_portfolio_overviews", description = """
            Get a summary of all portfolios belonging to the user (names, IDs, total values).
            Use this when asked: "How many portfolios do I have?",
            "List all my portfolios", "What are my portfolio names?"
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "overviewFallback")
    public String getPortfolioOverviews(
            @ToolParam(description = "User ID.") String userId) {
        try {
            String uid = resolve(userId);
            log.info("[MCP] get_portfolio_overviews called for userId: {} (resolved: {})", userId, uid);
            List<PortfolioBasicInfo> portfolios = portfolioManagementApi.getPortfolioBasicDetails(uid);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("portfolios", portfolios);
            result.put("count", portfolios.size());
            log.debug("[MCP] Returning {} portfolio overviews", portfolios.size());
            return response.toJson(result);
        } catch (Exception e) {
            log.error("Failed to fetch portfolio overviews for user {}", userId, e);
            throw new RuntimeException(e);
        }
    }

    public String overviewFallback(String userId, Exception e) {
        return response.unavailable("am-portfolio (portfolio overviews)");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String resolve(String userId) {
        return (userId != null && !userId.isBlank()) ? userId : props.getDefaults().getUserId();
    }
}
