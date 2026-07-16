package com.am.mcp.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Domain meters for Functional / Services dashboard (MCP tool path).
 *
 * <ul>
 *   <li>{@code tool.invocations} — tool calls by name/result</li>
 *   <li>{@code tool.duration} — tool latency timer by name</li>
 * </ul>
 */
@Component
public class McpBusinessMetrics {

    private final MeterRegistry registry;

    public McpBusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void toolInvocation(String tool, String result) {
        Counter.builder("tool.invocations")
                .description("MCP @Tool invocations")
                .tag("tool", tool == null ? "unknown" : tool)
                .tag("result", result == null ? "unknown" : result)
                .register(registry)
                .increment();
    }

    public void recordToolDuration(String tool, long durationNanos) {
        Timer.builder("tool.duration")
                .description("MCP @Tool execution duration")
                .tag("tool", tool == null ? "unknown" : tool)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }
}
