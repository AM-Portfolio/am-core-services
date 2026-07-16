package com.am.analysis.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Domain meters for Functional / Services dashboard (analysis streaming path).
 *
 * <ul>
 *   <li>{@code analysis.orchestrator.events} — consume/fanout outcomes by topic</li>
 *   <li>{@code analysis.dashboard.widget.publish} — widget Kafka publishes</li>
 *   <li>{@code analysis.portfolio.stream.publish} — portfolio stream publishes</li>
 * </ul>
 */
@Component
public class AnalysisBusinessMetrics {

    private final MeterRegistry registry;

    public AnalysisBusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void orchestratorEvent(String topic, String result) {
        Counter.builder("analysis.orchestrator.events")
                .description("Demand-driven orchestrator consume / fanout outcomes")
                .tag("topic", topic == null ? "unknown" : topic)
                .tag("result", result == null ? "unknown" : result)
                .register(registry)
                .increment();
    }

    public void dashboardWidgetPublish(String widget, String result) {
        Counter.builder("analysis.dashboard.widget.publish")
                .description("Dashboard widget Kafka publishes")
                .tag("widget", widget == null ? "unknown" : widget)
                .tag("result", result == null ? "unknown" : result)
                .register(registry)
                .increment();
    }

    public void portfolioStreamPublish(String result) {
        Counter.builder("analysis.portfolio.stream.publish")
                .description("Portfolio stream Kafka publishes")
                .tag("result", result == null ? "unknown" : result)
                .register(registry)
                .increment();
    }
}
