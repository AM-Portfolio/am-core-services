package com.am.mcp.tools;

import com.am.mcp.client.PortfolioRawClient;
import com.am.mcp.util.ResponseHelper;
import com.am.portfolio.client.api.PortfolioAnalyticsApi;
import com.am.portfolio.client.api.PortfolioManagementApi;
import com.am.portfolio.client.model.AdvancedAnalyticsRequest;
import com.am.portfolio.client.model.PortfolioBasicInfo;
import com.am.portfolio.client.model.PortfolioHoldings;
import com.am.portfolio.client.model.PortfolioModelV1;
import com.am.portfolio.client.model.PortfolioSummaryV1;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Portfolio domain MCP tools.
 * Identity comes from the inbound user JWT (AuthTokenProvider → Bearer).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.portfolio", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PortfolioTools {

    private final PortfolioManagementApi portfolioManagementApi;
    private final PortfolioAnalyticsApi portfolioAnalyticsApi;
    private final PortfolioRawClient portfolioRawClient;
    private final ResponseHelper response;

    @Tool(name = "get_portfolio_summary", description = """
            [portfolio] Get overall portfolio performance for the authenticated user.
            Returns: total invested value, current market value, unrealised P&L (amount + %),
            and day change. Use this when asked:
              "What is my portfolio value?", "How is my portfolio performing?",
              "What are my total returns?", "How much have I made/lost?"
            Optional portfolioId: pass a specific portfolio UUID, or omit for all portfolios.
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "portfolioSummaryFallback")
    public String getPortfolioSummary(
            @ToolParam(required = false, description = "Optional portfolio UUID. Omit to summarise all portfolios.") String portfolioId) {
        try {
            String pid = blankToNull(portfolioId);
            log.info("[MCP] get_portfolio_summary portfolioId={}", pid);
            Map<String, Object> summary = portfolioRawClient.getPortfolioSummary(pid);
            return response.toJson(slimSummary(summary));
        } catch (Exception e) {
            log.error("Failed to fetch portfolio summary", e);
            return response.errorJson("get_portfolio_summary", e);
        }
    }

    public String portfolioSummaryFallback(String portfolioId, Exception e) {
        return response.unavailable("am-portfolio (portfolio summary)");
    }

    /**
     * Keep chat/MCP payloads under max response size: scalars + broker names only
     * (full marketCap/sector trees are huge).
     */
    static Map<String, Object> slimSummary(Map<String, Object> summary) {
        Map<String, Object> slim = new LinkedHashMap<>();
        for (String key : List.of(
                "investmentValue",
                "currentValue",
                "totalGainLoss",
                "totalGainLossPercentage",
                "todayGainLoss",
                "todayGainLossPercentage",
                "totalAssets",
                "gainersCount",
                "losersCount",
                "todayGainersCount",
                "todayLosersCount",
                "lastUpdated")) {
            if (summary.containsKey(key)) {
                slim.put(key, summary.get(key));
            }
        }
        Object brokers = summary.get("brokerPortfolios");
        if (brokers instanceof Map<?, ?> brokerMap && !brokerMap.isEmpty()) {
            slim.put("brokers", brokerMap.keySet());
        }
        return slim;
    }

    /** @deprecated typed-model path kept for unit tests of scalar shaping */
    static Map<String, Object> slimSummary(PortfolioSummaryV1 summary) {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("investmentValue", summary.getInvestmentValue());
        raw.put("currentValue", summary.getCurrentValue());
        raw.put("totalGainLoss", summary.getTotalGainLoss());
        raw.put("totalGainLossPercentage", summary.getTotalGainLossPercentage());
        raw.put("todayGainLoss", summary.getTodayGainLoss());
        raw.put("todayGainLossPercentage", summary.getTodayGainLossPercentage());
        raw.put("totalAssets", summary.getTotalAssets());
        raw.put("gainersCount", summary.getGainersCount());
        raw.put("losersCount", summary.getLosersCount());
        raw.put("todayGainersCount", summary.getTodayGainersCount());
        raw.put("todayLosersCount", summary.getTodayLosersCount());
        raw.put("lastUpdated", summary.getLastUpdated());
        if (summary.getBrokerPortfolios() != null) {
            raw.put("brokerPortfolios", summary.getBrokerPortfolios());
        }
        return slimSummary(raw);
    }

    @Tool(name = "get_holdings", description = """
            [portfolio] Get the complete list of all stocks and ETFs in the authenticated user's portfolio.
            Returns for each holding: symbol, name, quantity, average cost, current value,
            total P&L (amount + %), and day change. Use this when asked:
              "What stocks do I own?", "Show me my holdings", "What is in my portfolio?"
            Optional portfolioId: pass a specific portfolio UUID, or omit for all.
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "holdingsFallback")
    public String getHoldings(
            @ToolParam(required = false, description = "Optional portfolio UUID. Omit for all portfolios.") String portfolioId) {
        try {
            String pid = blankToNull(portfolioId);
            log.info("[MCP] get_holdings portfolioId={}", pid);
            PortfolioHoldings holdings = portfolioManagementApi.getPortfolioHoldings(
                    pid, null, null, null);
            return response.toJson(holdings);
        } catch (Exception e) {
            log.error("Failed to fetch holdings", e);
            return response.errorJson("get_holdings", e);
        }
    }

    public String holdingsFallback(String portfolioId, Exception e) {
        return response.unavailable("am-portfolio (holdings)");
    }

    @Tool(name = "get_holding_detail", description = """
            [portfolio] Get detailed P&L and metrics for a specific stock in the authenticated user's portfolio.
            Returns: quantity, average cost, current price, invested amount, current value,
            unrealised P&L, sector, market cap type. Use this when asked:
              "How is my RELIANCE doing?", "What is my profit on HDFC Bank?",
              "Tell me about my TCS holding."
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "holdingDetailFallback")
    public String getHoldingDetail(
            @ToolParam(description = "Stock symbol (e.g. 'RELIANCE', 'HDFC', 'TCS').") String symbol) {
        try {
            List<PortfolioBasicInfo> portfolios = portfolioManagementApi.getPortfolioBasicDetails();
            for (PortfolioBasicInfo p : portfolios) {
                PortfolioHoldings holdings = portfolioManagementApi.getPortfolioHoldings(
                        p.getPortfolioId(), null, null, null);
                if (holdings.getEquityHoldings() != null) {
                    var match = holdings.getEquityHoldings().stream()
                            .filter(h -> h.getSymbol() != null && h.getSymbol().equalsIgnoreCase(symbol))
                            .findFirst();
                    if (match.isPresent()) {
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("portfolioId", p.getPortfolioId());
                        result.put("holding", match.get());
                        return response.toJson(result);
                    }
                }
            }
            return response.failure("get_holding_detail", "NOT_FOUND",
                    "No holding found matching '" + symbol + "'", false,
                    "Check the symbol with get_holdings or search_instruments.");
        } catch (Exception e) {
            log.error("Failed to fetch holding detail for symbol {}", symbol, e);
            return response.errorJson("get_holding_detail", e);
        }
    }

    public String holdingDetailFallback(String symbol, Exception e) {
        return response.unavailable("am-portfolio (holding detail)");
    }

    @Tool(name = "get_portfolio_overviews", description = """
            [portfolio] Get a summary of all portfolios belonging to the authenticated user (names, IDs, total values).
            Use this when asked: "How many portfolios do I have?",
            "List all my portfolios", "What are my portfolio names?"
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "overviewFallback")
    public String getPortfolioOverviews() {
        try {
            log.info("[MCP] get_portfolio_overviews called");
            List<PortfolioBasicInfo> portfolios = portfolioManagementApi.getPortfolioBasicDetails();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("portfolios", portfolios);
            result.put("count", portfolios.size());
            return response.toJson(result);
        } catch (Exception e) {
            log.error("Failed to fetch portfolio overviews", e);
            return response.errorJson("get_portfolio_overviews", e);
        }
    }

    public String overviewFallback(Exception e) {
        return response.unavailable("am-portfolio (portfolio overviews)");
    }

    @Tool(name = "get_portfolio_by_id", description = """
            [portfolio] Get the full portfolio model for a specific portfolio UUID.
            Use this when asked: "Show portfolio details for <id>",
            "Open portfolio X", after get_portfolio_overviews returns an ID.
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "byIdFallback")
    public String getPortfolioById(
            @ToolParam(description = "Portfolio UUID.") String portfolioId) {
        try {
            PortfolioModelV1 model = portfolioManagementApi.getPortfolioById(portfolioId);
            return response.toJson(model);
        } catch (Exception e) {
            log.error("Failed to fetch portfolio by id {}", portfolioId, e);
            return response.errorJson("get_portfolio_by_id", e);
        }
    }

    public String byIdFallback(String portfolioId, Exception e) {
        return response.unavailable("am-portfolio (by id)");
    }

    @Tool(name = "get_portfolio_advanced_analytics", description = """
            [portfolio] Get advanced analytics (performance, risk features) for a portfolio over a date range.
            Use this when asked: "Show advanced analytics for my portfolio",
            "Portfolio risk metrics", "Deep dive analytics for portfolio X".
            Defaults to last 30 days if fromDate/toDate omitted.
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "advancedAnalyticsFallback")
    public String getPortfolioAdvancedAnalytics(
            @ToolParam(description = "Portfolio UUID.") String portfolioId,
            @ToolParam(required = false, description = "Start date YYYY-MM-DD (optional).") String fromDate,
            @ToolParam(required = false, description = "End date YYYY-MM-DD (optional).") String toDate) {
        try {
            AdvancedAnalyticsRequest req = new AdvancedAnalyticsRequest();
            LocalDate to = (toDate != null && !toDate.isBlank())
                    ? LocalDate.parse(toDate) : LocalDate.now();
            LocalDate from = (fromDate != null && !fromDate.isBlank())
                    ? LocalDate.parse(fromDate) : to.minusDays(30);
            req.setFromDate(from);
            req.setToDate(to);
            req.setTimeFrame(AdvancedAnalyticsRequest.TimeFrameEnum._1_M);
            return response.toJson(portfolioAnalyticsApi.getAdvancedAnalytics(portfolioId, req));
        } catch (Exception e) {
            log.error("Failed advanced analytics for {}", portfolioId, e);
            return response.errorJson("get_portfolio_advanced_analytics", e);
        }
    }

    public String advancedAnalyticsFallback(String portfolioId, String fromDate, String toDate, Exception e) {
        return response.unavailable("am-portfolio (advanced analytics)");
    }

    static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
