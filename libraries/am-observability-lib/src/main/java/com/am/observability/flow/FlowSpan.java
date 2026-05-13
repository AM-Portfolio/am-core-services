package com.am.observability.flow;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * AutoCloseable handle returned by {@link FlowLogger#start(String, Object...)}.
 * Tracks step name, start time, and the MDC keys it has stamped so that
 * {@link #close()} can restore the previous MDC state (supports nested spans).
 *
 * <p>If {@link #close()} runs and neither {@link FlowLogger#complete} nor
 * {@link FlowLogger#fail} was called, the span auto-emits a status=ok line so
 * we never lose a flow checkpoint.</p>
 */
public final class FlowSpan implements AutoCloseable {

    private final FlowLogger logger;
    private final String stepName;
    private final long startNanos;
    private final Map<String, String> priorMdc;
    private final Map<String, String> initialFields;

    private boolean closed;

    FlowSpan(FlowLogger logger, String stepName, long startNanos,
             Map<String, String> priorMdc, Map<String, String> initialFields) {
        this.logger = logger;
        this.stepName = stepName;
        this.startNanos = startNanos;
        this.priorMdc = priorMdc;
        this.initialFields = initialFields;
    }

    public String stepName() {
        return stepName;
    }

    public long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public Map<String, String> initialFields() {
        return initialFields;
    }

    boolean isClosed() {
        return closed;
    }

    void markClosed() {
        this.closed = true;
    }

    void restoreMdc() {
        for (String key : initialFields.keySet()) {
            String prior = priorMdc.get(key);
            if (prior == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, prior);
            }
        }
        for (String reserved : new String[]{"flow.step", "flow.id"}) {
            String prior = priorMdc.get(reserved);
            if (prior == null) {
                MDC.remove(reserved);
            } else {
                MDC.put(reserved, prior);
            }
        }
    }

    /**
     * If the span has not been completed or failed explicitly, auto-emit a
     * status=ok close. Always restores prior MDC.
     */
    @Override
    public void close() {
        if (!closed) {
            logger.autoComplete(this);
        }
        restoreMdc();
    }

    static Map<String, String> snapshotMdc(String... keys) {
        Map<String, String> snapshot = new HashMap<>(keys.length);
        for (String key : keys) {
            snapshot.put(key, MDC.get(key));
        }
        return snapshot;
    }
}
