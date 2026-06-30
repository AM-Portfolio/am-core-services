package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.service.load.AnalysisEntityLoadService;
import com.am.kafka.config.AnalysisEntityKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolves the combined (global) portfolio {@link AnalysisEntity} for dashboard widget compute.
 */
@Service
@RequiredArgsConstructor
public class GlobalPortfolioResolver {

    private final AnalysisEntityLoadService entityLoadService;

    public Optional<AnalysisEntity> resolveGlobal(String userId) {
        return entityLoadService.loadGlobalPortfolio(userId);
    }

    public String globalSourceId() {
        return AnalysisEntityKeys.GLOBAL_SOURCE_ID;
    }
}
