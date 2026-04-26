package com.am.mcp.tools;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisHolding;
import com.am.analysis.adapter.repository.AnalysisRepository;
import com.am.mcp.config.AmMcpProperties;
import com.am.mcp.util.ResponseHelper;
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
 * Analysis domain MCP tools.
 *
 * Entity graph: AnalysisEntity → List<AnalysisHolding>
 * AnalysisHolding.classification → AssetClassification (sector, industry,
 * marketCapType)
 * AnalysisHolding.investment → InvestmentStats (investmentValue, currentValue,
 * profitLoss, …)
 * AnalysisEntity.performance → PerformanceSummary (totalGainLossPercentage,
 * dayChange, …)
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "am.tools.analysis", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AnalysisTools {

    private final AnalysisRepository analysisRepository;
    private final AmMcpProperties props;
    private final ResponseHelper response;

    @Tool(name = "get_top_movers", description = """
            Get the top 5 gainers and top 5 losers in the user's portfolio by total P&L percentage.
            Returns: symbol, gain/loss %, current value, day change. Use this when asked:
              "What are my best performers?", "Which stocks are losing the most?",
              "Show my top gainers and losers", "What's going up in my portfolio?"
            """)
    @CircuitBreaker(name = "am-analysis", fallbackMethod = "topMoversFallback")
    public String getTopMovers(
            @ToolParam(description = "User ID.") String userId,
            @ToolParam(description = "Ignored for now — returns all-time P&L ranking.") String timeFrame) {
        String uid = resolve(userId);
        log.info("[MCP] get_top_movers called for userId: {} (resolved: {})", userId, uid);
        List<AnalysisEntity> all = analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.HOLDING);

        List<Map<String, Object>> slim = all.stream()
                .filter(e -> e.getPerformance() != null && e.getPerformance().getTotalGainLossPercentage() != null)
                .map(e -> Map.<String, Object>of(
                        "symbol", e.getSourceId() != null ? e.getSourceId() : "",
                        "gainLossPct", e.getPerformance().getTotalGainLossPercentage(),
                        "dayChangePct",
                        e.getPerformance().getDayChangePercentage() != null
                                ? e.getPerformance().getDayChangePercentage()
                                : 0.0,
                        "totalValue",
                        e.getPerformance().getTotalValue() != null ? e.getPerformance().getTotalValue() : 0.0))
                .sorted((a, b) -> Double.compare(
                        ((Number) b.get("gainLossPct")).doubleValue(),
                        ((Number) a.get("gainLossPct")).doubleValue()))
                .collect(Collectors.toList());

        Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        int size = slim.size();
        resultMap.put("gainers", slim.subList(0, Math.min(5, size)));
        resultMap.put("losers", slim.subList(Math.max(0, size - 5), size));
        return response.toJson(resultMap);
    }

    public String topMoversFallback(String u, String t, Exception e) {
        log.error("[MCP] Fallback triggered for get_top_movers. User: {}, Error: {}", u, e.getMessage());
        return response.unavailable("am-analysis (top movers)");
    }

    @Tool(name = "get_sector_allocation", description = """
            Get the sector-wise breakdown of the user's portfolio (IT, Banking, Pharma, etc.).
            Returns: sector name → number of holdings. Use this when asked:
              "What sectors am I invested in?", "Am I diversified?",
              "What is my sector exposure?", "Am I overweight in IT or Banking?"
            """)
    @CircuitBreaker(name = "am-analysis", fallbackMethod = "sectorFallback")
    public String getSectorAllocation(
            @ToolParam(description = "User ID.") String userId) {
        String uid = resolve(userId);
        log.info("[MCP] get_sector_allocation called for userId: {} (resolved: {})", userId, uid);
        List<AnalysisEntity> entities = analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.HOLDING);
        Map<String, Long> sectorMap = entities.stream()
                .filter(e -> e.getHoldings() != null)
                .flatMap(e -> e.getHoldings().stream())
                .filter(h -> h.getClassification() != null && h.getClassification().getSector() != null)
                .collect(Collectors.groupingBy(h -> h.getClassification().getSector(), Collectors.counting()));
        Map<String, Object> sectorResult = new java.util.LinkedHashMap<>();
        sectorResult.put("sectorAllocation", sectorMap);
        sectorResult.put("sectorsCount", sectorMap.size());
        return response.toJson(sectorResult);
    }

    public String sectorFallback(String u, Exception e) {
        return response.unavailable("am-analysis (sector)");
    }

    @Tool(name = "get_market_cap_allocation", description = """
            Get large cap, mid cap, and small cap distribution of the portfolio by holding count.
            Use this when asked: "What is my market cap exposure?",
            "Am I in large caps or small caps?", "Show market cap breakdown."
            """)
    @CircuitBreaker(name = "am-analysis", fallbackMethod = "marketCapFallback")
    public String getMarketCapAllocation(
            @ToolParam(description = "User ID.") String userId) {
        String uid = resolve(userId);
        List<AnalysisEntity> entities = analysisRepository.findByOwnerIdAndType(uid, AnalysisEntityType.HOLDING);
        Map<String, Long> capMap = entities.stream()
                .filter(e -> e.getHoldings() != null)
                .flatMap(e -> e.getHoldings().stream())
                .filter(h -> h.getClassification() != null && h.getClassification().getMarketCapType() != null)
                .collect(Collectors.groupingBy(h -> h.getClassification().getMarketCapType(), Collectors.counting()));
        return response.toJson(Map.of("marketCapAllocation", capMap));
    }

    public String marketCapFallback(String u, Exception e) {
        return response.unavailable("am-analysis (market cap)");
    }

    private String resolve(String userId) {
        return (userId != null && !userId.isBlank()) ? userId : props.getDefaults().getUserId();
    }
}
