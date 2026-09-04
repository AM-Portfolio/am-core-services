package com.am.mcp.tools;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.mcp.config.AmMcpProperties;
import com.am.mcp.util.PortfolioAnalysisAggregator;
import com.am.mcp.util.ResponseHelper;
import com.am.mcp.util.UserIdResolver;
import com.am.portfolio.client.api.PortfolioAnalyticsApi;
import com.am.portfolio.client.api.PortfolioManagementApi;
import com.am.portfolio.client.model.AdvancedAnalyticsRequest;
import com.am.portfolio.client.model.PortfolioBasicInfo;
import com.am.portfolio.client.model.PortfolioHoldings;
import com.am.portfolio.client.model.PortfolioModelV1;
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
 * Identity: inbound JWT sub, else optional userId arg (fin-agent sends JWT sub because
 * MCP tool execution often runs off the SSE request thread).
 * Summary/holdings are sourced from am-analysis HOLDING entities (equity book),
 * not am-portfolio's raw model (which can include stale F&amp;O rows).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.portfolio", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PortfolioTools {

    private final PortfolioManagementApi portfolioManagementApi;
    private final PortfolioAnalyticsApi portfolioAnalyticsApi;
    private final AnalysisRepository analysisRepository;
    private final AmMcpProperties props;
    private final ResponseHelper response;

    @Tool(name = "get_portfolio_summary", description = """
            [portfolio] Get overall portfolio performance for the authenticated user.
            Returns: total invested value, current market value, unrealised P&L (amount + %),
            and day change. Use this when asked:
              "What is my portfolio value?", "How is my portfolio performing?",
              "What are my total returns?", "How much have I made/lost?"
            Optional portfolioId: pass a specific portfolio UUID, or omit for all portfolios.
            """)
    @CircuitBreaker(name = "am-analysis", fallbackMethod = "portfolioSummaryFallback")
    public String getPortfolioSummary(
            @ToolParam(description = "Optional portfolio UUID. Omit to summarise all portfolios.", required = false)
            String portfolioId,
            @ToolParam(description = "Authenticated user id. Ignored when a user JWT is present on the request.",
                    required = false)
            String userId) {
        try {
            String uid = UserIdResolver.resolve(userId, props);
            log.info("[MCP] get_portfolio_summary userId={} portfolioId={}", uid, portfolioId);
            List<AnalysisEntity> entities = PortfolioAnalysisAggregator.filterByPortfolioId(
                    analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.HOLDING),
                    portfolioId);
            // Equity books are stored as PORTFOLIO entities with nested holdings when HOLDING is empty.
            if (entities.isEmpty() || PortfolioAnalysisAggregator.listHoldings(entities).isEmpty()) {
                List<AnalysisEntity> portfolios = PortfolioAnalysisAggregator.filterByPortfolioId(
                        analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.PORTFOLIO),
                        portfolioId);
                if (!portfolios.isEmpty()) {
                    entities = portfolios;
                }
            }
            return response.toJson(PortfolioAnalysisAggregator.summarize(entities));
        } catch (Exception e) {
            log.error("Failed to fetch portfolio summary", e);
            return response.errorJson("get_portfolio_summary", e);
        }
    }

    public String portfolioSummaryFallback(String portfolioId, String userId, Exception e) {
        return response.unavailable("am-analysis (portfolio summary)");
    }

    @Tool(name = "get_holdings", description = """
            [portfolio] Get the complete list of all stocks and ETFs in the authenticated user's portfolio.
            Returns for each holding: symbol, name, quantity, average cost, current value,
            total P&L (amount + %), and day change. Use this when asked:
              "What stocks do I own?", "Show me my holdings", "What is in my portfolio?"
            Optional portfolioId: pass a specific portfolio UUID, or omit for all.
            """)
    @CircuitBreaker(name = "am-analysis", fallbackMethod = "holdingsFallback")
    public String getHoldings(
            @ToolParam(description = "Optional portfolio UUID. Omit for all portfolios.", required = false)
            String portfolioId,
            @ToolParam(description = "Authenticated user id. Ignored when a user JWT is present on the request.",
                    required = false)
            String userId) {
        try {
            String uid = UserIdResolver.resolve(userId, props);
            log.info("[MCP] get_holdings userId={} portfolioId={}", uid, portfolioId);
            List<AnalysisEntity> entities = PortfolioAnalysisAggregator.filterByPortfolioId(
                    analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.HOLDING),
                    portfolioId);
            if (entities.isEmpty() || PortfolioAnalysisAggregator.listHoldings(entities).isEmpty()) {
                List<AnalysisEntity> portfolios = PortfolioAnalysisAggregator.filterByPortfolioId(
                        analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.PORTFOLIO),
                        portfolioId);
                if (!portfolios.isEmpty()) {
                    entities = portfolios;
                }
            }
            List<Map<String, Object>> holdings = PortfolioAnalysisAggregator.listHoldings(entities);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("holdings", holdings);
            result.put("count", holdings.size());
            return response.toJson(result);
        } catch (Exception e) {
            log.error("Failed to fetch holdings", e);
            return response.errorJson("get_holdings", e);
        }
    }

    public String holdingsFallback(String portfolioId, String userId, Exception e) {
        return response.unavailable("am-analysis (holdings)");
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
            @ToolParam(description = "Start date YYYY-MM-DD (optional).") String fromDate,
            @ToolParam(description = "End date YYYY-MM-DD (optional).") String toDate) {
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
}
