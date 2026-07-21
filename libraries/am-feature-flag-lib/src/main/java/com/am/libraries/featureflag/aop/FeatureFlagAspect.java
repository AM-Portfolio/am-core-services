package com.am.libraries.featureflag.aop;

import com.am.libraries.featureflag.annotation.FallbackStrategy;
import com.am.libraries.featureflag.annotation.FeatureFlag;
import com.am.libraries.featureflag.service.GrowthBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aspect to intercept methods decorated with @FeatureFlag and apply fallback strategies if disabled.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagAspect {

    private final GrowthBookService growthBookService;

    @Around("@annotation(featureFlag)")
    public Object checkFeatureFlag(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) throws Throwable {
        if (!growthBookService.isOn(featureFlag.key())) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Class<?> returnType = method.getReturnType();

            log.info("Feature flag [{}] is disabled. Executing fallback strategy [{}] on method [{}]",
                    featureFlag.key(), featureFlag.fallback(), method.getName());

            return handleFallback(featureFlag.fallback(), returnType);
        }
        return joinPoint.proceed();
    }

    private Object handleFallback(FallbackStrategy strategy, Class<?> returnType) throws Throwable {
        if (strategy == FallbackStrategy.THROW_EXCEPTION) {
            throw new IllegalStateException("Operation blocked: Feature flag is disabled.");
        }
        
        if (strategy == FallbackStrategy.RETURN_FALSE) {
            if (returnType.equals(boolean.class) || returnType.equals(Boolean.class)) {
                return false;
            }
            log.warn("Fallback strategy RETURN_FALSE requested, but return type is {}. Falling back to NULL.", returnType.getName());
            return null;
        }

        if (strategy == FallbackStrategy.RETURN_EMPTY_LIST) {
            if (List.class.isAssignableFrom(returnType)) {
                return Collections.emptyList();
            }
            if (Set.class.isAssignableFrom(returnType)) {
                return Collections.emptySet();
            }
            if (Map.class.isAssignableFrom(returnType)) {
                return Collections.emptyMap();
            }
            log.warn("Fallback strategy RETURN_EMPTY_LIST requested, but return type is {}. Falling back to NULL.", returnType.getName());
            return null;
        }

        // Default to RETURN_NULL
        return null;
    }
}
