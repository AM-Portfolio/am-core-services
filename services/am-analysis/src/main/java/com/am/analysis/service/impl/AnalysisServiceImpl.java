package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.adapter.model.DashboardWidgetType;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.AnalysisService;
import com.am.analysis.service.DashboardSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    private final AllocationAnalysisService allocationService;
    private final PerformanceAnalysisService performanceService;
    private final TopMoversAnalysisService topMoversService;
    private final DashboardSnapshotService snapshotService;

    @Override
    public AllocationResponse getAllocation(String id, AnalysisEntityType type, String userId, AnalysisGroupBy groupBy) {
        log.info("Request received: Get Allocation - ID: {}, Type: {}, User: {}, GroupBy: {}", id, type, userId, groupBy);
        boolean isDashboardDefault = "ALL".equals(id) && type == AnalysisEntityType.PORTFOLIO && (groupBy == null || groupBy == AnalysisGroupBy.SECTOR);
        if (isDashboardDefault) {
            return snapshotService.load(userId, DashboardWidgetType.ALLOCATION, AllocationResponse.class)
                    .orElseGet(() -> {
                        log.info("[Allocation] Snapshot miss for user {}, computing live", userId);
                        AllocationResponse response = allocationService.getAllocation(id, type, userId, groupBy);
                        if (response != null) {
                            snapshotService.persist(userId, DashboardWidgetType.ALLOCATION, response);
                        }
                        return response;
                    });
        }
        return allocationService.getAllocation(id, type, userId, groupBy);
    }

    @Override
    public PerformanceResponse getPerformance(String id, AnalysisEntityType type, String timeFrame, String userId) {
        log.info("Request received: Get Performance - ID: {}, Type: {}, TimeFrame: {}, User: {}", id, type, timeFrame, userId);
        boolean isDashboardDefault = id == null && type == AnalysisEntityType.PORTFOLIO && "1M".equalsIgnoreCase(timeFrame);
        if (isDashboardDefault) {
            return snapshotService.load(userId, DashboardWidgetType.HISTORY, PerformanceResponse.class)
                    .orElseGet(() -> {
                        log.info("[History] Snapshot miss for user {}, computing live", userId);
                        PerformanceResponse response = performanceService.getPerformance(id, type, timeFrame, userId);
                        if (response != null) {
                            snapshotService.persist(userId, DashboardWidgetType.HISTORY, response);
                        }
                        return response;
                    });
        }
        return performanceService.getPerformance(id, type, timeFrame, userId);
    }

    @Override
    public TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId, AnalysisGroupBy groupBy) {
        log.info("Request received: Get Top Movers - ID: {}, Type: {}, TimeFrame: {}, User: {}, GroupBy: {}", id, type, timeFrame, userId, groupBy);
        boolean isDashboardDefault = id == null && type == AnalysisEntityType.PORTFOLIO && "1D".equalsIgnoreCase(timeFrame) && groupBy == AnalysisGroupBy.STOCK;
        if (isDashboardDefault) {
            Optional<TopMoversResponse> cached = snapshotService.load(userId, DashboardWidgetType.MOVERS, TopMoversResponse.class);
            if (cached.isPresent() && hasMoverEntries(cached.get())) {
                return cached.get();
            }
            log.info("[Movers] Snapshot miss or empty for user {}, computing live", userId);
            TopMoversResponse response = topMoversService.getTopMovers(id, type, timeFrame, userId, groupBy);
            if (response != null && hasMoverEntries(response)) {
                snapshotService.persist(userId, DashboardWidgetType.MOVERS, response);
            }
            return response;
        }
        return topMoversService.getTopMovers(id, type, timeFrame, userId, groupBy);
    }

    private static boolean hasMoverEntries(TopMoversResponse response) {
        return response != null
                && ((response.getGainers() != null && !response.getGainers().isEmpty())
                || (response.getLosers() != null && !response.getLosers().isEmpty()));
    }
}

