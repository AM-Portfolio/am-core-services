package com.am.analysis.controller;

import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.dto.ActivityFilter;
import com.am.analysis.dto.ActivitySortBy;
import com.am.analysis.dto.ActivityStatus;
import com.am.analysis.dto.ActivityType;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.DashboardSummary;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.RecentActivityResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.AnalysisService;
import com.am.analysis.service.DashboardAnalysisService;
import com.am.domain.trade.PortfolioOverview;
import com.am.kafka.config.Timeframe;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.security.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "Analysis", description = "Dashboard and entity analysis APIs")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final DashboardAnalysisService dashboardService;
    private final FlowLogger flowLogger;

    @Operation(
            summary = "Dashboard summary",
            description = "Aggregate portfolio metrics for the authenticated user.",
            operationId = "getDashboardSummary")
    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.summary", "userId", userId)) {
            DashboardSummary summary = dashboardService.getSummary(userId);
            flowLogger.complete(span,
                    "portfolios", summary == null ? 0 : summary.getTotalPortfolios(),
                    "holdings", summary == null ? 0 : summary.getTotalHoldings(),
                    "isComplete", summary != null && summary.isComplete());
            return ResponseEntity.ok(summary);
        }
    }

    @Operation(
            summary = "Portfolio overviews",
            description = "List portfolio overview cards; optionally filter by portfolio id.",
            operationId = "getPortfolioOverviews")
    @GetMapping("/dashboard/portfolio-overviews")
    public ResponseEntity<List<PortfolioOverview>> getPortfolioOverviews(
            @Parameter(description = "Optional portfolio id filter", example = "pf-demo-001")
            @RequestParam(name = "portfolioId", required = false) String portfolioId) {
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.portfolio_overviews",
                "userId", userId, "portfolioId", portfolioId == null ? "ALL" : portfolioId)) {
            List<PortfolioOverview> overviews = dashboardService.getPortfolioOverviews(userId);
            if (portfolioId != null && !portfolioId.isBlank()) {
                overviews = overviews.stream()
                        .filter(p -> portfolioId.equals(p.getPortfolioId()))
                        .collect(Collectors.toList());
            }
            flowLogger.complete(span, "overviews", overviews.size());
            return ResponseEntity.ok(overviews);
        }
    }

    @Operation(
            summary = "Publish dashboard update",
            description = "Trigger a dashboard refresh publication for the authenticated user.",
            operationId = "publishDashboardUpdate")
    @PostMapping("/dashboard/publish-update")
    public ResponseEntity<Void> publishDashboardUpdate() {
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.publish_update", "userId", userId)) {
            dashboardService.publishDashboardUpdate(userId);
            flowLogger.complete(span);
            return ResponseEntity.ok().build();
        }
    }

    @Operation(
            summary = "Dashboard top movers",
            description = "Top movers across the user's portfolios for the given window.",
            operationId = "getDashboardTopMovers")
    @GetMapping("/dashboard/top-movers")
    public ResponseEntity<TopMoversResponse> getDashboardTopMovers(
            @Parameter(description = "Performance window (wire code)", example = "1D")
            @RequestParam(name = "timeFrame", required = false, defaultValue = "1D") Timeframe timeFrame) {
        String tf = timeFrame.getCode();
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.top_movers",
                "userId", userId, "timeFrame", tf)) {
            TopMoversResponse response = analysisService.getTopMovers(null, AnalysisEntityType.PORTFOLIO, tf, userId,
                    AnalysisGroupBy.STOCK);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @Operation(
            summary = "Dashboard performance",
            description = "Aggregate performance series for the user's portfolios.",
            operationId = "getDashboardPerformance")
    @GetMapping("/dashboard/performance")
    public ResponseEntity<PerformanceResponse> getDashboardPerformance(
            @Parameter(description = "Performance window (wire code)", example = "1M")
            @RequestParam(name = "timeFrame", required = false, defaultValue = "1M") Timeframe timeFrame) {
        String tf = timeFrame.getCode();
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.performance",
                "userId", userId, "timeFrame", tf)) {
            PerformanceResponse response = analysisService.getPerformance(null, AnalysisEntityType.PORTFOLIO, tf, userId);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @Operation(
            summary = "Recent activity",
            description = "Paginated recent holding / portfolio activity with optional filters.",
            operationId = "getRecentActivity")
    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<RecentActivityResponse> getRecentActivity(
            @Parameter(description = "Activity type filter")
            @RequestParam(name = "type", required = false) ActivityType type,
            @Parameter(description = "P&L status filter")
            @RequestParam(name = "status", required = false) ActivityStatus status,
            @Parameter(description = "Sector filter", example = "Technology")
            @RequestParam(name = "sector", required = false) String sector,
            @Parameter(description = "Portfolio name partial match", example = "Growth")
            @RequestParam(name = "portfolioName", required = false) String portfolioName,
            @Parameter(description = "Sort field")
            @RequestParam(name = "sortBy", required = false, defaultValue = "TIMESTAMP") ActivitySortBy sortBy,
            @Parameter(description = "Zero-based page", example = "0")
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "10")
            @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        String userId = UserContext.getUserIdOrThrow();
        String typeName = type == null ? null : type.name();
        String statusName = status == null ? null : status.name();
        String sortName = sortBy == null ? ActivitySortBy.TIMESTAMP.name() : sortBy.name();

        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.recent_activity",
                "userId", userId,
                "page", page,
                "size", size,
                "sortBy", sortName,
                "type", typeName,
                "status", statusName)) {
            ActivityFilter filter = ActivityFilter.builder()
                    .type(typeName)
                    .status(statusName)
                    .sector(sector)
                    .portfolioName(portfolioName)
                    .sortBy(sortName)
                    .page(page)
                    .size(size)
                    .build();
            RecentActivityResponse response = dashboardService.getRecentActivity(userId, filter);
            flowLogger.complete(span,
                    "items", response == null || response.getItems() == null ? 0 : response.getItems().size());
            return ResponseEntity.ok(response);
        }
    }

    @Operation(
            summary = "Entity allocation",
            description = "Allocation breakdown for a portfolio or other analysis entity.",
            operationId = "getAllocation")
    @GetMapping("/{type}/{id}/allocation")
    public ResponseEntity<AllocationResponse> getAllocation(
            @Parameter(description = "Entity kind", example = "PORTFOLIO")
            @PathVariable("type") AnalysisEntityType type,
            @Parameter(description = "Entity id (open catalog)", example = "pf-demo-001")
            @PathVariable("id") String id,
            @Parameter(description = "Optional grouping")
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        AnalysisGroupBy groupBy = paramGroupBy;
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.allocation",
                "type", type.name(),
                "id", id,
                "userId", userId,
                "groupBy", groupBy == null ? "DEFAULT" : groupBy.name())) {
            AllocationResponse response = analysisService.getAllocation(id, type, userId, groupBy);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @Operation(
            summary = "Entity performance",
            description = "Performance series for a specific analysis entity.",
            operationId = "getPerformance")
    @GetMapping("/{type}/{id}/performance")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @Parameter(description = "Entity kind", example = "PORTFOLIO")
            @PathVariable("type") AnalysisEntityType type,
            @Parameter(description = "Entity id (open catalog)", example = "pf-demo-001")
            @PathVariable("id") String id,
            @Parameter(description = "Performance window (wire code)", example = "1M")
            @RequestParam(value = "timeFrame", defaultValue = "1M") Timeframe timeFrame) {
        String tf = timeFrame.getCode();
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.performance.entity",
                "type", type.name(),
                "id", id,
                "userId", userId,
                "timeFrame", tf)) {
            PerformanceResponse response = analysisService.getPerformance(id, type, tf, userId);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @Operation(
            summary = "Top movers by category",
            description = "Top movers for all entities of the given kind owned by the user.",
            operationId = "getTopMoversByCategory")
    @GetMapping("/{type}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByCategory(
            @Parameter(description = "Entity kind", example = "PORTFOLIO")
            @PathVariable("type") AnalysisEntityType type,
            @Parameter(description = "Performance window (wire code)", example = "1D")
            @RequestParam(value = "timeFrame", required = false) Timeframe timeFrame,
            @Parameter(description = "Grouping; defaults to STOCK")
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        AnalysisGroupBy groupBy = paramGroupBy != null ? paramGroupBy : AnalysisGroupBy.STOCK;
        String tf = timeFrame == null ? null : timeFrame.getCode();
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.top_movers.category",
                "type", type.name(),
                "userId", userId,
                "groupBy", groupBy.name(),
                "timeFrame", tf)) {
            TopMoversResponse response = analysisService.getTopMovers(null, type, tf, userId, groupBy);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @Operation(
            summary = "Top movers by entity",
            description = "Top movers within a specific analysis entity.",
            operationId = "getTopMoversByEntity")
    @GetMapping("/{type}/{id}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByEntity(
            @Parameter(description = "Entity kind", example = "PORTFOLIO")
            @PathVariable("type") AnalysisEntityType type,
            @Parameter(description = "Entity id (open catalog)", example = "pf-demo-001")
            @PathVariable("id") String id,
            @Parameter(description = "Performance window (wire code)", example = "1D")
            @RequestParam(value = "timeFrame", required = false) Timeframe timeFrame,
            @Parameter(description = "Grouping; defaults to STOCK")
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        AnalysisGroupBy groupBy = paramGroupBy != null ? paramGroupBy : AnalysisGroupBy.STOCK;
        String tf = timeFrame == null ? null : timeFrame.getCode();
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.top_movers.entity",
                "type", type.name(),
                "id", id,
                "userId", userId,
                "groupBy", groupBy.name(),
                "timeFrame", tf)) {
            TopMoversResponse response = analysisService.getTopMovers(id, type, tf, userId, groupBy);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }
}
