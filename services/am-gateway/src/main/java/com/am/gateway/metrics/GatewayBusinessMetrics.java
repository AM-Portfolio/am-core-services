package com.am.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Domain meters for Functional / Services dashboard (gateway streaming path).
 *
 * <ul>
 *   <li>{@code websocket.sessions.active} — live STOMP sessions</li>
 *   <li>{@code stomp.connect} — CONNECT attempts by result</li>
 *   <li>{@code stream.relay.messages} — Kafka→WS relay by destination/result</li>
 * </ul>
 */
@Component
public class GatewayBusinessMetrics {

    private final AtomicInteger activeSessions = new AtomicInteger();
    private final MeterRegistry registry;

    public GatewayBusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("websocket.sessions.active", activeSessions, AtomicInteger::doubleValue)
                .description("Active STOMP WebSocket sessions")
                .register(registry);
    }

    public void sessionConnected() {
        activeSessions.incrementAndGet();
    }

    public void sessionDisconnected() {
        activeSessions.updateAndGet(n -> Math.max(0, n - 1));
    }

    public void stompConnect(String result) {
        Counter.builder("stomp.connect")
                .description("STOMP CONNECT attempts")
                .tag("result", result == null ? "unknown" : result)
                .register(registry)
                .increment();
    }

    public void kafkaRelay(String destination, String result) {
        Counter.builder("stream.relay.messages")
                .description("Kafka messages relayed to WebSocket clients")
                .tag("destination", destination == null ? "unknown" : destination)
                .tag("result", result == null ? "unknown" : result)
                .register(registry)
                .increment();
    }
}
