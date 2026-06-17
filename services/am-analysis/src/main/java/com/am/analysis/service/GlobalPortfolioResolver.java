package com.am.analysis.service;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.repository.AnalysisRepository;
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

    private final AnalysisRepository analysisRepository;

    public Optional<AnalysisEntity> resolveGlobal(String userId) {
        return analysisRepository.findById(AnalysisEntityKeys.globalEntityId(userId))
                .filter(entity -> userId.equals(entity.getOwnerId()));
    }

    public String globalSourceId() {
        return AnalysisEntityKeys.GLOBAL_SOURCE_ID;
    }
}
