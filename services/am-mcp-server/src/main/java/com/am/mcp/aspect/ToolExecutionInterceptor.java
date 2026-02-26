package com.am.mcp.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * AOP interceptor — wraps every @Tool method with structured logging and timing.
 *
 * Automatically captures for every tool call:
 *   - tool name + class
 *   - duration in milliseconds
 *   - outcome (OK / ERROR)
 *   - character count of response (shows if truncation is needed)
 *
 * No manual logging needed in tool classes — this handles it centrally.
 */
@Slf4j
@Aspect
@Component
public class ToolExecutionInterceptor {

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object traceToolExecution(ProceedingJoinPoint pjp) throws Throwable {
        String toolClass  = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();

        // Resolve @Tool name for richer logs
        MethodSignature sig  = (MethodSignature) pjp.getSignature();
        Tool            tool = sig.getMethod().getAnnotation(Tool.class);
        String          toolName = (tool != null && !tool.name().isBlank())
                ? tool.name() : methodName;

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long   ms     = System.currentTimeMillis() - start;
            int    chars  = result instanceof String s ? s.length() : -1;

            log.info("tool={} class={} duration={}ms chars={} status=OK",
                     toolName, toolClass, ms, chars);

            return result;

        } catch (Throwable t) {
            long ms = System.currentTimeMillis() - start;
            log.error("tool={} class={} duration={}ms status=ERROR error={}",
                      toolName, toolClass, ms, t.getMessage());
            throw t;    // re-throw so Spring AI MCP handles error serialization
        }
    }
}
