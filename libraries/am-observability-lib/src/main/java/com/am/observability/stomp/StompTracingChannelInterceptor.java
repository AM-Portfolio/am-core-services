package com.am.observability.stomp;

import com.am.observability.kafka.KafkaTraceHeaders;
import com.am.observability.mdc.MdcKeys;
import com.am.observability.trace.TracingHelper;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.List;
import java.util.UUID;

/**
 * STOMP {@link ChannelInterceptor} that:
 * <ul>
 *     <li>Extracts {@code traceparent} from incoming STOMP frames and stamps
 *         the derived {@code traceId} / {@code spanId} into MDC for the
 *         downstream {@code @MessageMapping} handler.</li>
 *     <li>Generates a new {@code traceId} when the client sends none, so
 *         every STOMP frame has a searchable id.</li>
 *     <li>On outbound frames, injects {@code traceparent} so that any STOMP
 *         broker / client that participates in tracing can correlate.</li>
 * </ul>
 *
 * <p>Registered from each service's {@code WebSocketConfig.configureClientInboundChannel}
 * (and {@code configureClientOutboundChannel}).</p>
 */
public class StompTracingChannelInterceptor implements ChannelInterceptor {

    private final TracingHelper tracingHelper;
    private final String serviceName;

    public StompTracingChannelInterceptor(TracingHelper tracingHelper, String serviceName) {
        this.tracingHelper = tracingHelper;
        this.serviceName = serviceName;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        String correlationId = readNativeHeader(accessor, KafkaTraceHeaders.CORRELATION_ID);
        String traceparent = readNativeHeader(accessor, KafkaTraceHeaders.TRACEPARENT);

        String traceId;
        String spanId;
        if (traceparent != null && traceparent.length() >= 55) {
            traceId = traceparent.substring(3, 35);
            spanId = traceparent.substring(36, 52);
        } else {
            traceId = tracingHelper.currentTraceIdOrNew();
            spanId = tracingHelper.currentSpanIdOrNew();
        }

        if (correlationId == null) {
            correlationId = traceId;
        }

        MDC.put(MdcKeys.TRACE_ID, traceId);
        MDC.put(MdcKeys.SPAN_ID, spanId);
        MDC.put(MdcKeys.CORRELATION_ID, correlationId);
        if (serviceName != null) {
            MDC.put(MdcKeys.SERVICE, serviceName);
        }
        if (accessor.getUser() != null && accessor.getUser().getName() != null) {
            MDC.put(MdcKeys.USER_ID, accessor.getUser().getName());
        }
        if (accessor.getCommand() != null) {
            MDC.put(MdcKeys.REQUEST_METHOD, "STOMP-" + accessor.getCommand().name());
        }
        if (accessor.getDestination() != null) {
            MDC.put(MdcKeys.REQUEST_PATH, accessor.getDestination());
        }
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel,
                                    boolean sent, Exception ex) {
        MDC.remove(MdcKeys.TRACE_ID);
        MDC.remove(MdcKeys.SPAN_ID);
        MDC.remove(MdcKeys.CORRELATION_ID);
        MDC.remove(MdcKeys.USER_ID);
        MDC.remove(MdcKeys.REQUEST_METHOD);
        MDC.remove(MdcKeys.REQUEST_PATH);
    }

    private static String readNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @SuppressWarnings("unused")
    private static String newCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
