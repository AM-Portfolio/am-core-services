package com.am.analysis.controller;

import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.dto.ActivityFilter;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.DashboardSummary;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.RecentActivityResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.AnalysisService;
import com.am.analysis.service.DashboardAnalysisService;
import com.am.domain.trade.PortfolioOverview;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.security.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
public class AnalysisController {

    private final AnalysisService analysisService;
    private final DashboardAnalysisService dashboardService;
    private final FlowLogger flowLogger;

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

    @GetMapping("/dashboard/portfolio-overviews")
    public ResponseEntity<List<PortfolioOverview>> getPortfolioOverviews(
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

    @PostMapping("/dashboard/publish-update")
    public ResponseEntity<Void> publishDashboardUpdate() {
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.publish_update", "userId", userId)) {
            dashboardService.publishDashboardUpdate(userId);
            flowLogger.complete(span);
            return ResponseEntity.ok().build();
        }
    }

    @GetMapping("/dashboard/top-movers")
    public ResponseEntity<TopMoversResponse> getDashboardTopMovers(
            @RequestParam(name = "timeFrame", required = false, defaultValue = "1D") String timeFrame) {
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.top_movers",
                "userId", userId, "timeFrame", timeFrame)) {
            TopMoversResponse response = analysisService.getTopMovers(null, AnalysisEntityType.PORTFOLIO, timeFrame, userId,
                    AnalysisGroupBy.STOCK);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/dashboard/performance")
    public ResponseEntity<PerformanceResponse> getDashboardPerformance(
            @RequestParam(name = "timeFrame", required = false, defaultValue = "1M") String timeFrame) {
        String userId = UserContext.getUserIdOrThrow();
        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.performance",
                "userId", userId, "timeFrame", timeFrame)) {
            PerformanceResponse response = analysisService.getPerformance(null, AnalysisEntityType.PORTFOLIO, timeFrame, userId);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<RecentActivityResponse> getRecentActivity(
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sector", required = false) String sector,
            @RequestParam(name = "portfolioName", required = false) String portfolioName,
            @RequestParam(name = "sortBy", required = false, defaultValue = "TIMESTAMP") String sortBy,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        String userId = UserContext.getUserIdOrThrow();

        try (FlowSpan span = flowLogger.start("analysis.http.dashboard.recent_activity",
                "userId", userId,
                "page", page,
                "size", size,
                "sortBy", sortBy,
                "type", type,
                "status", status)) {
            ActivityFilter filter = ActivityFilter.builder()
                    .type(type)
                    .status(status)
                    .sector(sector)
                    .portfolioName(portfolioName)
                    .sortBy(sortBy)
                    .page(page)
                    .size(size)
                    .build();
            RecentActivityResponse response = dashboardService.getRecentActivity(userId, filter);
            flowLogger.complete(span,
                    "items", response == null || response.getItems() == null ? 0 : response.getItems().size());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/{type}/{id}/allocation")
    public ResponseEntity<AllocationResponse> getAllocation(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        AnalysisGroupBy groupBy = paramGroupBy;
        String userId = UserContext.getUserIdOrThrow();
        AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
        try (FlowSpan span = flowLogger.start("analysis.http.allocation",
                "type", entityType.name(),
                "id", id,
                "userId", userId,
                "groupBy", groupBy == null ? "DEFAULT" : groupBy.name())) {
            AllocationResponse response = analysisService.getAllocation(id, entityType, userId, groupBy);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/{type}/{id}/performance")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "timeFrame", defaultValue = "1M") String timeFrame) {
        String userId = UserContext.getUserIdOrThrow();
        AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
        try (FlowSpan span = flowLogger.start("analysis.http.performance.entity",
                "type", entityType.name(),
                "id", id,
                "userId", userId,
                "timeFrame", timeFrame)) {
            PerformanceResponse response = analysisService.getPerformance(id, entityType, timeFrame, userId);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }
    

    @GetMapping("/{type}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByCategory(
            @PathVariable("type") String type,
            @RequestParam(value = "timeFrame", required = false) String timeFrame,
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        AnalysisGroupBy groupBy = paramGroupBy != null ? paramGroupBy : AnalysisGroupBy.STOCK;
        String userId = UserContext.getUserIdOrThrow();
        AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
        try (FlowSpan span = flowLogger.start("analysis.http.top_movers.category",
                "type", entityType.name(),
                "userId", userId,
                "groupBy", groupBy.name(),
                "timeFrame", timeFrame)) {
            TopMoversResponse response = analysisService.getTopMovers(null, entityType, timeFrame, userId, groupBy);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/{type}/{id}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByEntity(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "timeFrame", required = false) String timeFrame,
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        AnalysisGroupBy groupBy = paramGroupBy != null ? paramGroupBy : AnalysisGroupBy.STOCK;
        String userId = UserContext.getUserIdOrThrow();
        AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
        try (FlowSpan span = flowLogger.start("analysis.http.top_movers.entity",
                "type", entityType.name(),
                "id", id,
                "userId", userId,
                "groupBy", groupBy.name(),
                "timeFrame", timeFrame)) {
            TopMoversResponse response = analysisService.getTopMovers(id, entityType, timeFrame, userId, groupBy);
            flowLogger.complete(span);
            return ResponseEntity.ok(response);
        }
    }
}
