package com.am.libraries.featureflag.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Decorator annotation to check a GrowthBook feature flag before executing the method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureFlag {
    /**
     * The key of the feature flag in GrowthBook (e.g. "redis-enabled").
     */
    String key();

    /**
     * The strategy to execute if the feature flag is disabled.
     */
    FallbackStrategy fallback() default FallbackStrategy.RETURN_NULL;
}
