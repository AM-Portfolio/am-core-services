package com.am.scheduler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable both execution tracking (audit logging to MongoDB)
 * and distributed locking (using ShedLock) for a scheduled method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackedAndLockedScheduler {

    /**
     * The unique name of the scheduler and lock.
     * This is used as the lock key and the job name in audit records.
     */
    String name();

    /**
     * How long the lock should be kept in case the executing instance dies.
     * Support formats: Spring-style duration (e.g., "15m", "30s") or ISO-8601 (e.g., "PT15M").
     */
    String lockAtMostFor() default "15m";

    /**
     * The minimum amount of time the lock should be kept.
     * Prevents execution from running again if clock drift occurs between instances.
     * Support formats: Spring-style duration (e.g., "1m", "10s") or ISO-8601 (e.g., "PT1M").
     */
    String lockAtLeastFor() default "1m";
}
