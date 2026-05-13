package com.am.observability.mdc;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Decorator that copies the calling thread's MDC into the executing thread
 * before running the task and restores the worker thread's MDC afterwards.
 * Apply to any custom {@link org.springframework.core.task.AsyncTaskExecutor}
 * or {@link java.util.concurrent.Executor} bean so {@code traceId}/{@code spanId}
 * and {@code flow.*} keys survive {@code @Async} and {@code CompletableFuture} boundaries.
 */
public final class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
