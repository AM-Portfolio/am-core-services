package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.adapter.model.AnalysisGroupBy;
import com.am.analysis.dto.AllocationResponse;
import com.am.analysis.dto.PerformanceResponse;
import com.am.analysis.dto.TopMoversResponse;

public interface AnalysisService {
    AllocationResponse getAllocation(String id, AnalysisEntityType type, String userId, AnalysisGroupBy groupBy);
    PerformanceResponse getPerformance(String id, AnalysisEntityType type, String timeFrame, String userId);
    TopMoversResponse getTopMovers(String id, AnalysisEntityType type, String timeFrame, String userId, AnalysisGroupBy groupBy);
}
