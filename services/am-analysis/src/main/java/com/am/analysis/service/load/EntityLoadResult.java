package com.am.analysis.service.load;

import com.am.analysis.adapter.model.AnalysisEntity;

import java.util.List;

public record EntityLoadResult(
        List<AnalysisEntity> entities,
        boolean empty,
        boolean bootstrapRequested
) {
    public static EntityLoadResult of(List<AnalysisEntity> entities, boolean bootstrapRequested) {
        boolean isEmpty = entities == null || entities.isEmpty();
        return new EntityLoadResult(
                isEmpty ? List.of() : List.copyOf(entities),
                isEmpty,
                bootstrapRequested);
    }

    public static EntityLoadResult empty(boolean bootstrapRequested) {
        return new EntityLoadResult(List.of(), true, bootstrapRequested);
    }
}
