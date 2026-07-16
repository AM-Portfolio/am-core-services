package com.am.observability.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;

import java.util.Collections;
import java.util.Map;

/**
 * Renames Micrometer meters by exact local name → canonical name so services can
 * keep legacy builders while Prometheus scrapes concept-based series used by
 * shared Grafana dashboards.
 *
 * <p>Configured via {@code am.observability.metrics.map}. Empty map is identity.</p>
 */
public final class MetricNameMappingFilter implements MeterFilter {

    private final Map<String, String> map;

    public MetricNameMappingFilter(Map<String, String> map) {
        this.map = map == null || map.isEmpty() ? Collections.emptyMap() : Map.copyOf(map);
    }

    @Override
    public Meter.Id map(Meter.Id id) {
        if (map.isEmpty()) {
            return id;
        }
        String canonical = map.get(id.getName());
        if (canonical == null || canonical.isBlank() || canonical.equals(id.getName())) {
            return id;
        }
        return id.withName(canonical);
    }
}
