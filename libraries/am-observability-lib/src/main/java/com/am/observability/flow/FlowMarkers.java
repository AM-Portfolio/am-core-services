package com.am.observability.flow;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * SLF4J markers used to tag business-flow log lines.
 * Aggregators (Kibana, Loki, Datadog) can filter on {@code marker:FLOW} to
 * separate flow events from chatty technical logs.
 */
public final class FlowMarkers {

    private FlowMarkers() {
    }

    public static final Marker FLOW = MarkerFactory.getMarker("FLOW");
}
