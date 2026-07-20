package com.am.observability.config;

import com.am.observability.trace.IgnoreTracing;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * [WHY THIS WAS ADDED]:
 * This class is the core of our "Zero-Configuration" policy. It intercepts method calls in
 * standard business service classes automatically so developers don't have to annotate everything manually.
 *
 * [WHAT IT DOES]:
 * Uses Spring AspectJ AOP (Aspect-Oriented Programming) to dynamically wrap public methods in `@Service`
 * classes inside a tracing span, recording start/end execution times.
 */
@Aspect
public class AutoObservabilityAspect {

    private final ObservationRegistry observationRegistry;

    public AutoObservabilityAspect(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    /**
     * Intercepts all public methods of classes annotated with @Service.
     * The pointcut expression matches: any class marked with @Service (within), and any public method (execution).
     */
    @Around("within(@org.springframework.stereotype.Service *) && execution(public * *(..))")
    public Object traceServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // [SAFEGUARD]: If the method or class is tagged with @IgnoreTracing, execute it directly.
        // This prevents creating endless logs/spans for loops or high-frequency methods.
        if (method.isAnnotationPresent(IgnoreTracing.class) || targetClass.isAnnotationPresent(IgnoreTracing.class)) {
            return joinPoint.proceed();
        }

        // Generate a name (e.g. "PortfolioService.enrichHoldings") to display in Grafana Tempo
        String name = targetClass.getSimpleName() + "." + method.getName();
        
        // Start a stopwatch, run the actual Java method, and stop the stopwatch when finished.
        return Observation.createNotStarted(name, observationRegistry)
                .contextualName(name)
                .observeChecked(() -> joinPoint.proceed());
    }
}
