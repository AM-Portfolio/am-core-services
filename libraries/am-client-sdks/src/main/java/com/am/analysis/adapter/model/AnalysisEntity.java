package com.am.analysis.adapter.model;

import com.am.analysis.adapter.model.components.Lifecycle;
import com.am.analysis.adapter.model.components.PerformanceSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analysis_data")
public class AnalysisEntity {
    @Id
    private String id; // Composite ID or Unique ID from source
    
    private String sourceId; // e.g., Portfolio ID, Symbol
    private AnalysisEntityType type;
    private String ownerId; // User ID
    
    // Performance Metrics Grouped
    private PerformanceSummary performance;

    // Standardized Holdings for Allocation Analysis
    private List<AnalysisHolding> holdings;
    
    // Flexible Stats for specific needs
    private Map<String, Object> additionalStats;
    
    // Periods
    private Lifecycle lifecycle;
    
    private LocalDateTime lastUpdated;


}

