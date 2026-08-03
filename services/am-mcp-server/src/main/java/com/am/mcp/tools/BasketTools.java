package com.am.mcp.tools;

import com.am.mcp.client.BasketApiClient;
import com.am.mcp.config.AmMcpProperties;
import com.am.mcp.util.PayloadSlim;
import com.am.mcp.util.ResponseHelper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basket domain MCP tools — ETF replication / exposure / allocation.
 * calculate_basket_quantities rebuilds opportunity server-side from etfIsin + portfolioId.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.basket", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class BasketTools {

    private final BasketApiClient basketApiClient;
    private final AmMcpProperties props;
    private final ResponseHelper response;

    @Tool(name = "get_basket_opportunities", description = """
            [basket] Find ETF basket opportunities that match the user's holdings.
            Use this when asked: "Which ETFs match my holdings?", "Basket opportunities",
            "Find ETF replication opportunities for my portfolio."
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "opportunitiesFallback")
    public String getBasketOpportunities(
            @ToolParam(description = "Portfolio UUID.") String portfolioId,
            @ToolParam(required = false, description = "Optional ETF search query (e.g. 'NIFTY').") String etfQuery,
            @ToolParam(required = false, description = "Optional user ID; defaults from JWT.") String userId) {
        try {
            Map<String, Object> body = baseBody(portfolioId, userId);
            if (etfQuery != null && !etfQuery.isBlank()) {
                body.put("etfQuery", etfQuery);
            }
            body.put("userHoldings", List.of());
            return response.toJson(PayloadSlim.slimBasket(
                    basketApiClient.getOpportunities(body), PayloadSlim.BASKET_LIMIT));
        } catch (Exception e) {
            return response.errorJson("get_basket_opportunities", e);
        }
    }

    public String opportunitiesFallback(String p, String e, String u, Exception ex) {
        return response.unavailable("am-portfolio (basket opportunities)");
    }

    @Tool(name = "get_basket_exposure", description = """
            [basket] Calculate cumulative stock/sector exposure across direct holdings and ETF holdings.
            Use this when asked: "What is my true exposure including ETFs?", "Cumulative basket exposure."
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "exposureFallback")
    public String getBasketExposure(
            @ToolParam(description = "Portfolio UUID.") String portfolioId,
            @ToolParam(required = false, description = "Optional user ID; defaults from JWT.") String userId) {
        try {
            Map<String, Object> body = baseBody(portfolioId, userId);
            body.put("userHoldings", List.of());
            return response.toJson(PayloadSlim.slimBasket(
                    basketApiClient.getExposure(body), PayloadSlim.BASKET_LIMIT));
        } catch (Exception e) {
            return response.errorJson("get_basket_exposure", e);
        }
    }

    public String exposureFallback(String p, String u, Exception e) {
        return response.unavailable("am-portfolio (basket exposure)");
    }

    @Tool(name = "get_basket_allocations", description = """
            [basket] Get portfolio allocation breakdown (direct vs indirect via ETFs) for UI visualization.
            Use this when asked: "Show basket allocations", "Direct vs ETF allocation breakdown."
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "allocationsFallback")
    public String getBasketAllocations(
            @ToolParam(description = "Portfolio UUID.") String portfolioId,
            @ToolParam(required = false, description = "Optional user ID; defaults from JWT.") String userId) {
        try {
            return response.toJson(PayloadSlim.slimBasket(
                    basketApiClient.getAllocations(baseBody(portfolioId, userId)), PayloadSlim.BASKET_LIMIT));
        } catch (Exception e) {
            return response.errorJson("get_basket_allocations", e);
        }
    }

    public String allocationsFallback(String p, String u, Exception e) {
        return response.unavailable("am-portfolio (basket allocations)");
    }

    @Tool(name = "get_basket_preview", description = """
            [basket] Preview how an ETF basket maps to the user's holdings (composition + weights).
            Use this when asked: "Preview NIFTYBEES basket", "Show ETF composition vs my holdings."
            For share quantities, call calculate_basket_quantities with the same etfIsin + portfolioId + amount
            (server rebuilds opportunity; do not copy this JSON into another tool).
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "previewFallback")
    public String getBasketPreview(
            @ToolParam(description = "ETF ISIN.") String etfIsin,
            @ToolParam(description = "Portfolio UUID.") String portfolioId,
            @ToolParam(required = false, description = "Optional user ID; defaults from JWT.") String userId) {
        try {
            Map<String, Object> body = baseBody(portfolioId, userId);
            body.put("etfIsin", etfIsin);
            body.put("userHoldings", List.of());
            return response.toJson(PayloadSlim.slimBasket(
                    basketApiClient.getPreview(body), PayloadSlim.BASKET_LIMIT));
        } catch (Exception e) {
            return response.errorJson("get_basket_preview", e);
        }
    }

    public String previewFallback(String i, String p, String u, Exception e) {
        return response.unavailable("am-portfolio (basket preview)");
    }

    @Tool(name = "calculate_basket_quantities", description = """
            [basket] Calculate share quantities for a target investment amount.
            Pass etfIsin + portfolioId + investmentAmount; the server fetches the basket opportunity
            (same as get_basket_preview) then calculates quantities. Do not pass JSON blobs.
            Use this when asked: "How many shares to buy for 50000 in this basket?"
            """)
    @CircuitBreaker(name = "am-portfolio", fallbackMethod = "calculateFallback")
    public String calculateBasketQuantities(
            @ToolParam(description = "Investment amount (must be > 0).") Double investmentAmount,
            @ToolParam(description = "ETF ISIN.") String etfIsin,
            @ToolParam(description = "Portfolio UUID.") String portfolioId,
            @ToolParam(required = false, description = "Optional user ID; defaults from JWT.") String userId) {
        try {
            if (investmentAmount == null || investmentAmount <= 0) {
                return response.failure("calculate_basket_quantities", "BAD_REQUEST",
                        "investmentAmount is required and must be > 0", false, null);
            }
            if (etfIsin == null || etfIsin.isBlank() || portfolioId == null || portfolioId.isBlank()) {
                return response.failure("calculate_basket_quantities", "BAD_REQUEST",
                        "etfIsin and portfolioId are required", false, null);
            }
            Map<String, Object> previewBody = baseBody(portfolioId, userId);
            previewBody.put("etfIsin", etfIsin);
            previewBody.put("userHoldings", List.of());
            Object opportunity = basketApiClient.getPreview(previewBody);
            Map<String, Object> body = new HashMap<>();
            body.put("investmentAmount", investmentAmount);
            body.put("opportunity", opportunity);
            return response.toJson(PayloadSlim.slimBasket(
                    basketApiClient.calculateQuantities(body), PayloadSlim.BASKET_LIMIT));
        } catch (Exception e) {
            return response.errorJson("calculate_basket_quantities", e);
        }
    }

    public String calculateFallback(Double a, String i, String p, String u, Exception e) {
        return response.unavailable("am-portfolio (basket calculate)");
    }

    private Map<String, Object> baseBody(String portfolioId, String userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("portfolioId", portfolioId);
        body.put("userId", com.am.mcp.util.UserIdResolver.resolve(userId, props));
        return body;
    }
}
