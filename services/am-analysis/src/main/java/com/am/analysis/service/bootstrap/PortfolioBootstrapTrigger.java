package com.am.analysis.service.bootstrap;

import com.am.analysis.config.AnalysisBootstrapProperties;
import com.am.observability.flow.FlowLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debounced portfolio bootstrap via {@code am-trigger-calculation}.
 * Shared by STOMP subscribe path (orchestrator) and HTTP read path (entity load service).
 */
@Service
@RequiredArgsConstructor
public class PortfolioBootstrapTrigger {

    private final TriggerCalculationPublisher triggerCalculationPublisher;
    private final AnalysisBootstrapProperties bootstrapProperties;
    private final FlowLogger flowLogger;

    private final Map<String, Long> lastBootstrapTrigger = new ConcurrentHashMap<>();

    /**
     * @return true if a trigger was published, false if disabled or debounced
     */
    public boolean requestBootstrap(String userId, String portfolioId, String source, String inheritedTraceId) {
        if (!bootstrapProperties.isEnabled()) {
            return false;
        }
        if (userId == null || userId.isBlank()) {
            return false;
        }

        String bootstrapKey = userId + ":" + (portfolioId != null ? portfolioId : "global");
        long now = System.currentTimeMillis();
        Long last = lastBootstrapTrigger.get(bootstrapKey);

        if (last != null && (now - last) < bootstrapProperties.getDebounceMs()) {
            flowLogger.step("analysis.bootstrap.debounced",
                    "userId", userId,
                    "portfolioId", portfolioId != null ? portfolioId : "GLOBAL",
                    "source", source,
                    "window_ms", bootstrapProperties.getDebounceMs());
            return false;
        }

        lastBootstrapTrigger.put(bootstrapKey, now);
        triggerCalculationPublisher.publish(userId, portfolioId, source, inheritedTraceId);
        flowLogger.step("analysis.bootstrap.requested",
                "userId", userId,
                "portfolioId", portfolioId != null ? portfolioId : "GLOBAL",
                "source", source);
        return true;
    }
}
