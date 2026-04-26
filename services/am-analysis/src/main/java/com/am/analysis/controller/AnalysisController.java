package com.am.analysis.controller;

import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.dto.ActivityFilter;
import com.am.analysis.dto.ActivityItem;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.DashboardSummary;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.RecentActivityResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.AnalysisService;
import com.am.analysis.service.DashboardAnalysisService;
import com.am.domain.trade.PortfolioOverview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnalysisController {

    private final AnalysisService analysisService;
    private final DashboardAnalysisService dashboardService;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary(@RequestParam("userId") String userId) {
        log.info("[AnalysisController] GET /dashboard/summary for userId: {}", userId);
        return ResponseEntity.ok(dashboardService.getSummary(userId));
    }

    /**
     * Returns portfolio overview cards.
     * - portfolioId omitted → ALL portfolios (one card per portfolio)
     * - portfolioId provided → single portfolio detail view
     */
    @GetMapping("/dashboard/portfolio-overviews")
    public ResponseEntity<List<PortfolioOverview>> getPortfolioOverviews(
            @RequestParam("userId") String userId,
            @RequestParam(name = "portfolioId", required = false) String portfolioId) {
        log.info("[AnalysisController] GET /dashboard/portfolio-overviews for userId: {}, portfolioId: {}", userId, portfolioId);
        List<PortfolioOverview> overviews = dashboardService.getPortfolioOverviews(userId);
        if (portfolioId != null && !portfolioId.isBlank()) {
            overviews = overviews.stream()
                    .filter(p -> portfolioId.equals(p.getPortfolioId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(overviews);
    }

    @PostMapping("/dashboard/publish-update")
    public ResponseEntity<Void> publishDashboardUpdate(@RequestParam("userId") String userId) {
        log.info("[AnalysisController] POST /dashboard/publish-update for userId: {}", userId);
        dashboardService.publishDashboardUpdate(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard/top-movers")
    public ResponseEntity<TopMoversResponse> getDashboardTopMovers(
            @RequestParam("userId") String userId,
            @RequestParam(name = "timeFrame", required = false, defaultValue = "1D") String timeFrame) {
        log.info("[AnalysisController] GET /dashboard/top-movers for userId: {}, timeFrame: {}", userId, timeFrame);
        return ResponseEntity.ok(analysisService.getTopMovers(null, AnalysisEntityType.PORTFOLIO, timeFrame, userId,
                AnalysisGroupBy.STOCK));
    }

    @GetMapping("/dashboard/performance")
    public ResponseEntity<PerformanceResponse> getDashboardPerformance(
            @RequestParam("userId") String userId,
            @RequestParam(name = "timeFrame", required = false, defaultValue = "1M") String timeFrame) {
        return ResponseEntity.ok(analysisService.getPerformance(null, AnalysisEntityType.PORTFOLIO, timeFrame, userId));
    }

    /**
     * Paginated + filtered recent activity.
     * Supports: type, status (WIN/LOSS/NEUTRAL), sector, portfolioName, sortBy,
     * page, size.
     *
     * Example:
     * GET /api/v1/analysis/dashboard/recent-activity
     * ?userId=xxx&status=WIN&sortBy=PROFIT_LOSS&page=0&size=20
     */
    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<RecentActivityResponse> getRecentActivity(
            @RequestParam("userId") String userId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sector", required = false) String sector,
            @RequestParam(name = "portfolioName", required = false) String portfolioName,
            @RequestParam(name = "sortBy", required = false, defaultValue = "TIMESTAMP") String sortBy,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size) {

        log.info("[AnalysisController] GET /dashboard/recent-activity for userId: {}, page: {}, size: {}, sortBy: {}", 
                userId, page, size, sortBy);
        ActivityFilter filter = ActivityFilter.builder()
                .type(type)
                .status(status)
                .sector(sector)
                .portfolioName(portfolioName)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(dashboardService.getRecentActivity(userId, filter));
    }

    @GetMapping("/{type}/{id}/allocation")
    public ResponseEntity<AllocationResponse> getAllocation(
            @RequestHeader("Authorization") String token,
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestHeader(value = "groupBy", required = false) AnalysisGroupBy headerGroupBy,
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        try {
            AnalysisGroupBy groupBy = paramGroupBy != null ? paramGroupBy : headerGroupBy;
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getAllocation(id, entityType, userId, groupBy));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            e.printStackTrace(); // Log the full stack trace
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{type}/{id}/performance")
    public ResponseEntity<PerformanceResponse> getPerformance(
            @RequestHeader("Authorization") String token,
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "timeFrame", defaultValue = "1M") String timeFrame) {
        try {
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getPerformance(id, entityType, timeFrame, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{type}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByCategory(
            @RequestHeader("Authorization") String token,
            @PathVariable("type") String type,
            @RequestParam(value = "timeFrame", required = false) String timeFrame,
            @RequestHeader(value = "groupBy", required = false) AnalysisGroupBy headerGroupBy,
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        try {
            AnalysisGroupBy groupBy = paramGroupBy != null ? paramGroupBy
                    : (headerGroupBy != null ? headerGroupBy : AnalysisGroupBy.STOCK);
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getTopMovers(null, entityType, timeFrame, userId, groupBy));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{type}/{id}/top-movers")
    public ResponseEntity<TopMoversResponse> getTopMoversByEntity(
            @RequestHeader("Authorization") String token,
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "timeFrame", required = false) String timeFrame,
            @RequestHeader(value = "groupBy", required = false) AnalysisGroupBy headerGroupBy,
            @RequestParam(value = "groupBy", required = false) AnalysisGroupBy paramGroupBy) {
        try {
            AnalysisGroupBy groupBy = paramGroupBy != null ? paramGroupBy
                    : (headerGroupBy != null ? headerGroupBy : AnalysisGroupBy.STOCK);
            String userId = com.am.security.util.TokenExtractor.extractUserId(token);
            AnalysisEntityType entityType = AnalysisEntityType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(analysisService.getTopMovers(id, entityType, timeFrame, userId, groupBy));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}

// trigger -6
