package com.am.mcp.aspect;

import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import com.am.observability.mdc.MdcKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * AOP interceptor — wraps every @Tool method with FlowLogger checkpoints and
 * MDC enrichment. Captures tool name, class, duration, outcome and response
 * size; stamps {@code tool.name} + {@code tool.args.size} onto MDC for the
 * duration of each invocation so every log line emitted inside the tool can
 * be filtered by tool name.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ToolExecutionInterceptor {

    private final FlowLogger flowLogger;

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object traceToolExecution(ProceedingJoinPoint pjp) throws Throwable {
        String toolClass = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Tool tool = sig.getMethod().getAnnotation(Tool.class);
        String toolName = (tool != null && !tool.name().isBlank()) ? tool.name() : methodName;

        Object[] args = pjp.getArgs();
        int argSize = args == null ? 0 : args.length;

        String priorToolName = MDC.get(MdcKeys.TOOL_NAME);
        String priorArgsSize = MDC.get(MdcKeys.TOOL_ARGS_SIZE);
        MDC.put(MdcKeys.TOOL_NAME, toolName);
        MDC.put(MdcKeys.TOOL_ARGS_SIZE, String.valueOf(argSize));

        try (FlowSpan span = flowLogger.start("mcp.tool.invoke",
                "tool", toolName,
                "class", toolClass,
                "args", argSize)) {
            try {
                Object result = pjp.proceed();
                int chars = result instanceof String s ? s.length() : -1;
                flowLogger.complete(span,
                        "response_chars", chars,
                        "response_type", result == null ? "null" : result.getClass().getSimpleName());
                return result;
            } catch (Throwable t) {
                flowLogger.fail(span, t);
                throw t;
            }
        } finally {
            if (priorToolName == null) {
                MDC.remove(MdcKeys.TOOL_NAME);
            } else {
                MDC.put(MdcKeys.TOOL_NAME, priorToolName);
            }
            if (priorArgsSize == null) {
                MDC.remove(MdcKeys.TOOL_ARGS_SIZE);
            } else {
                MDC.put(MdcKeys.TOOL_ARGS_SIZE, priorArgsSize);
            }
        }
    }
}
