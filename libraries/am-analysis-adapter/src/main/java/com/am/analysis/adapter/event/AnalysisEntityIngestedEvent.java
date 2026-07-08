package com.am.analysis.adapter.event;

import com.am.analysis.adapter.model.AnalysisEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published after an {@link AnalysisEntity} is persisted to MongoDB.
 */
@Getter
public class AnalysisEntityIngestedEvent extends ApplicationEvent {

    private final AnalysisEntity entity;

    public AnalysisEntityIngestedEvent(Object source, AnalysisEntity entity) {
        super(source);
        this.entity = entity;
    }
}
