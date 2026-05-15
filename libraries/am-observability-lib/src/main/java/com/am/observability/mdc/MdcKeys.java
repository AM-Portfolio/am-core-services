package com.am.observability.mdc;

/**
 * Canonical MDC key names used across the observability library.
 * Keep this list in sync with docs/observability/ARCHITECTURE.md section 6.
 */
public final class MdcKeys {

    private MdcKeys() {
    }

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";

    public static final String SERVICE = "service";
    public static final String USER_ID = "userId";
    public static final String CORRELATION_ID = "correlationId";

    public static final String REQUEST_METHOD = "request.method";
    public static final String REQUEST_PATH = "request.path";
    public static final String HTTP_STATUS = "http.status";

    public static final String FLOW_ID = "flow.id";
    public static final String FLOW_STEP = "flow.step";
    public static final String FLOW_USER = "flow.user";
    public static final String FLOW_DURATION_MS = "flow.duration_ms";
    public static final String FLOW_OUTCOME = "flow.outcome";

    public static final String TOOL_NAME = "tool.name";
    public static final String TOOL_ARGS_SIZE = "tool.args.size";
}
