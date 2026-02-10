package com.am.analysis.adapter.repository;

import com.am.analysis.adapter.model.AnalysisEntity;
import com.am.analysis.adapter.model.AnalysisEntityType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRepository extends MongoRepository<AnalysisEntity, String> {
    List<AnalysisEntity> findByOwnerId(String ownerId);
    
    // Top Gainers
    List<AnalysisEntity> findTop10ByTypeOrderByTotalGainLossPercentageDesc(AnalysisEntityType type);
    
    // Top Losers
    List<AnalysisEntity> findTop10ByTypeOrderByTotalGainLossPercentageAsc(AnalysisEntityType type);

    // Batch lookup
    List<AnalysisEntity> findBySourceIdInAndType(List<String> sourceIds, AnalysisEntityType type);
}
