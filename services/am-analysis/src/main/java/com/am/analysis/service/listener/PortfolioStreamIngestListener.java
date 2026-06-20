package com.am.analysis.service.listener;

import com.am.analysis.adapter.event.AnalysisEntityIngestedEvent;
import com.am.analysis.adapter.model.AnalysisEntityType;
import com.am.analysis.service.PortfolioStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Pushes portfolio stream updates after Mongo ingest when the user is actively watching.
 */
@Component
@RequiredArgsConstructor
public class PortfolioStreamIngestListener {

    private final PortfolioStreamingService portfolioStreamingService;

    @EventListener
    public void onEntityIngested(AnalysisEntityIngestedEvent event) {
        if (event.getEntity() == null || event.getEntity().getType() != AnalysisEntityType.PORTFOLIO) {
            return;
        }
        portfolioStreamingService.publishIfUserWatching(event.getEntity());
    }
}
