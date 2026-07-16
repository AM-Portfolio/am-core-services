package com.am.observability.flow;

import com.am.observability.mdc.MdcKeys;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Human-readable end-to-end checkpoint helper. Each call emits a single
 * structured log line with marker {@link FlowMarkers#FLOW} and a fixed
 * {@code message} format so engineers can read flows by grep.
 *
 * <pre>
 * try (FlowSpan span = flowLogger.start("analysis.http.dashboard.summary",
 *         "userId", u)) {
 *     // ... business logic ...
 *     flowLogger.complete(span, "portfolios", count);
 * } catch (Exception e) {
 *     flowLogger.fail(span, e);
 *     throw e;
 * }
 * </pre>
 *
 * <p>Stamps MDC with {@code flow.id}, {@code flow.step}, {@code flow.user}
 * for the duration of the span; restores prior MDC on close.</p>
 */
public final class FlowLogger {

    private static final Logger LOG = LoggerFactory.getLogger(FlowLogger.class);

    private final Tracer tracer;
    private final ObservationRegistry observationRegistry;
    private final String serviceName;

    public FlowLogger(Tracer tracer, String serviceName) {
        this(tracer, ObservationRegistry.NOOP, serviceName);
    }

    public FlowLogger(Tracer tracer, ObservationRegistry observationRegistry, String serviceName) {
        this.tracer = tracer;
        this.observationRegistry = observationRegistry == null ? ObservationRegistry.NOOP : observationRegistry;
        this.serviceName = serviceName;
    }

    /**
     * Open a flow checkpoint. Returns an AutoCloseable that should be used in
     * a try-with-resources block; will auto-complete on close if neither
     * {@link #complete} nor {@link #fail} was called explicitly.
     */
    public FlowSpan start(String stepName, Object... kv) {
        long startNanos = System.nanoTime();
        Map<String, String> prior = FlowSpan.snapshotMdc(MdcKeys.FLOW_STEP, MdcKeys.FLOW_ID);
        Map<String, String> fields = toMap(kv);

        String flowId = MDC.get(MdcKeys.FLOW_ID);
        if (flowId == null || flowId.isEmpty()) {
            flowId = currentTraceId();
            if (flowId != null) {
                MDC.put(MdcKeys.FLOW_ID, flowId);
            }
        }
        MDC.put(MdcKeys.FLOW_STEP, stepName);

        String userId = fields.get("userId");
        if (userId == null) {
            userId = MDC.get(MdcKeys.USER_ID);
        }
        if (userId != null) {
            MDC.put(MdcKeys.FLOW_USER, userId);
            fields.putIfAbsent("user", userId);
        }

        Observation observation = Observation.createNotStarted(stepName, observationRegistry)
                .contextualName(stepName)
                .lowCardinalityKeyValue("service", serviceName);
        observation.start();
        // Open scope on the calling thread so downstream client spans (HTTP,
        // Kafka, Mongo, Redis) nest under this flow step in Tempo.
        Observation.Scope scope = observation.openScope();

        FlowSpan span = new FlowSpan(this, stepName, startNanos, prior, fields, observation, scope);
        emit(stepName, "start", null, fields, null);
        return span;
    }

    /**
     * Mid-span point-in-time checkpoint (no timer). Useful for marking phases
     * inside a longer step, e.g. "loaded {N} entities from Mongo".
     */
    public void step(String stepName, Object... kv) {
        emit(stepName, "step", null, toMap(kv), null);
    }

    /**
     * Close a span with status=ok and the elapsed duration.
     */
    public void complete(FlowSpan span, Object... kv) {
        if (span == null || span.isClosed()) {
            return;
        }
        long elapsed = span.elapsedMillis();
        Map<String, String> fields = mergeFields(span.initialFields(), toMap(kv));
        emit(span.stepName(), "ok", elapsed, fields, null);
        span.stopObservation(null);
        span.markClosed();
    }

    /**
     * Close a span with status=err. Logs the cause class + message.
     */
    public void fail(FlowSpan span, Throwable cause, Object... kv) {
        if (span == null || span.isClosed()) {
            return;
        }
        long elapsed = span.elapsedMillis();
        Map<String, String> fields = mergeFields(span.initialFields(), toMap(kv));
        if (cause != null) {
            fields.put("error", cause.getClass().getSimpleName());
            if (cause.getMessage() != null) {
                fields.put("error.message", truncate(cause.getMessage()));
            }
        }
        emit(span.stepName(), "err", elapsed, fields, cause);
        span.stopObservation(cause);
        span.markClosed();
    }

    /**
     * Called by {@link FlowSpan#close()} for try-with-resources blocks that
     * exit normally without an explicit complete/fail.
     */
    void autoComplete(FlowSpan span) {
        long elapsed = span.elapsedMillis();
        emit(span.stepName(), "ok", elapsed, span.initialFields(), null);
        span.stopObservation(null);
        span.markClosed();
    }

    private void emit(String stepName, String outcome, Long durationMs,
                      Map<String, String> fields, Throwable cause) {
        String priorOutcome = MDC.get(MdcKeys.FLOW_OUTCOME);
        String priorDuration = MDC.get(MdcKeys.FLOW_DURATION_MS);
        try {
            MDC.put(MdcKeys.FLOW_OUTCOME, outcome);
            if (durationMs != null) {
                MDC.put(MdcKeys.FLOW_DURATION_MS, String.valueOf(durationMs));
            }
            String summary = renderSummary(stepName, outcome, durationMs, fields);
            if ("err".equals(outcome)) {
                if (cause != null) {
                    LOG.error(FlowMarkers.FLOW, summary, cause);
                } else {
                    LOG.error(FlowMarkers.FLOW, summary);
                }
            } else {
                LOG.info(FlowMarkers.FLOW, summary);
            }
        } finally {
            if (priorOutcome == null) {
                MDC.remove(MdcKeys.FLOW_OUTCOME);
            } else {
                MDC.put(MdcKeys.FLOW_OUTCOME, priorOutcome);
            }
            if (priorDuration == null) {
                MDC.remove(MdcKeys.FLOW_DURATION_MS);
            } else {
                MDC.put(MdcKeys.FLOW_DURATION_MS, priorDuration);
            }
        }
    }

    private String renderSummary(String stepName, String outcome, Long durationMs,
                                 Map<String, String> fields) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[FLOW step=").append(stepName);
        String flowId = MDC.get(MdcKeys.FLOW_ID);
        if (flowId != null) {
            sb.append(" flow=").append(flowId);
        }
        String user = fields.get("user");
        if (user == null) {
            user = MDC.get(MdcKeys.USER_ID);
        }
        if (user != null) {
            sb.append(" user=").append(user);
        }
        sb.append(" status=").append(outcome);
        if (durationMs != null) {
            sb.append(" dur_ms=").append(durationMs);
        }
        sb.append("]");
        boolean hasExtras = false;
        StringBuilder extras = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            if ("user".equals(key)) {
                continue;
            }
            if (!hasExtras) {
                extras.append(' ');
                hasExtras = true;
            } else {
                extras.append(' ');
            }
            extras.append(key).append('=').append(entry.getValue());
        }
        if (hasExtras) {
            sb.append(extras);
        }
        return sb.toString();
    }

    private String currentTraceId() {
        if (tracer == null) {
            return null;
        }
        try {
            if (tracer.currentSpan() != null && tracer.currentSpan().context() != null) {
                return tracer.currentSpan().context().traceId();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public String serviceName() {
        return serviceName;
    }

    private static Map<String, String> toMap(Object... kv) {
        Map<String, String> out = new LinkedHashMap<>();
        if (kv == null) {
            return out;
        }
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object key = kv[i];
            Object value = kv[i + 1];
            if (key != null) {
                out.put(String.valueOf(key), value == null ? "null" : String.valueOf(value));
            }
        }
        return out;
    }

    private static Map<String, String> mergeFields(Map<String, String> base, Map<String, String> overlay) {
        Map<String, String> merged = new LinkedHashMap<>(base);
        merged.putAll(overlay);
        return merged;
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= 256) {
            return message;
        }
        return message.substring(0, 256) + "...(truncated)";
    }
}
