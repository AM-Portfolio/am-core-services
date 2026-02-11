package com.am.analysis.service.impl;

import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.TopMoversResponse;
import com.am.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    private final AllocationAnalysisService allocationService;
    private final PerformanceAnalysisService performanceService;
    private final TopMoversAnalysisService topMoversService;

    @Override
    public AllocationResponse getAllocation(String id, AnalysisEntityType type, String userId) {
        log.info("Request received: Get Allocation - ID: {}, Type: {}, User: {}", id, type, userId);
        return allocationService.getAllocation(id, type, userId);
    }

    @Override
    public PerformanceResponse getPerformance(String id, AnalysisEntityType type, String timeFrame, String userId) {
        log.info("Request received: Get Performance - ID: {}, Type: {}, TimeFrame: {}, User: {}", id, type, timeFrame, userId);
        return performanceService.getPerformance(id, type, timeFrame, userId);
    }

    @Override
    public TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId) {
        log.info("Request received: Get Top Movers - ID: {}, Type: {}, TimeFrame: {}, User: {}", id, type, timeFrame, userId);
        return topMoversService.getTopMovers(id, type, timeFrame, userId);
    }
}

