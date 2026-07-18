package com.am.observability.config;

import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;

/**
 * [WHY THIS WAS ADDED]:
 * Zero-Configuration bridge for Lettuce Redis. If any microservice defines their own
 * custom {@link LettuceConnectionFactory} bean, Spring Boot's default client configuration
 * is bypassed. This PostProcessor intercepts the factory bean *before* initialization
 * (before afterPropertiesSet) and programmatically overrides its ClientResources
 * configuration using a Dynamic Proxy to inject tracing.
 *
 * [WHY postProcessBeforeInitialization]:
 * LettuceConnectionFactory implements InitializingBean. During afterPropertiesSet() it
 * builds an internal connection pool using whatever ClientResources are configured at
 * that moment. If we inject tracing after afterPropertiesSet (postProcessAfterInitialization),
 * the connection pool is already built with an untraced ClientResources — Redis spans
 * won't appear in Tempo. Using postProcessBeforeInitialization guarantees our traced
 * ClientResources are in place BEFORE the internal pool is created.
 */
public class LettuceConnectionFactoryTracingPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(LettuceConnectionFactoryTracingPostProcessor.class);

    private final org.springframework.beans.factory.ObjectProvider<ClientResources> clientResourcesProvider;

    public LettuceConnectionFactoryTracingPostProcessor(org.springframework.beans.factory.ObjectProvider<ClientResources> clientResourcesProvider) {
        this.clientResourcesProvider = clientResourcesProvider;
    }

    /**
     * Runs BEFORE afterPropertiesSet() / SmartLifecycle.start().
     * This is the correct hook: we inject traced ClientResources into the
     * LettuceClientConfiguration before Lettuce builds its internal connection pool.
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof LettuceConnectionFactory factory) {
            try {
                log.info("[OBSERVABILITY] Intercepted LettuceConnectionFactory bean '{}' BEFORE initialization to inject tracing ClientResources", beanName);

                ClientResources clientResources = clientResourcesProvider.getIfAvailable();
                if (clientResources == null) {
                    log.warn("[OBSERVABILITY] ClientResources bean not available yet for '{}', skipping tracing injection", beanName);
                    return bean;
                }

                // Extract the LettuceClientConfiguration field (set before afterPropertiesSet builds the pool)
                Field configField = LettuceConnectionFactory.class.getDeclaredField("clientConfiguration");
                configField.setAccessible(true);
                LettuceClientConfiguration originalConfig = (LettuceClientConfiguration) configField.get(factory);

                if (originalConfig == null) {
                    log.warn("[OBSERVABILITY] LettuceClientConfiguration was null in factory bean '{}', skipping tracing injection", beanName);
                    return bean;
                }

                // Check if it already uses our traced ClientResources
                Optional<ClientResources> currentResources = originalConfig.getClientResources();
                if (currentResources.isPresent() && currentResources.get() == clientResources) {
                    log.debug("[OBSERVABILITY] LettuceConnectionFactory '{}' already has traced ClientResources, skipping", beanName);
                    return bean;
                }

                // Create a dynamic proxy that delegates everything to the original config
                // EXCEPT getClientResources() — which returns our traced instance.
                // This ensures all other config (SSL, timeouts, etc.) is preserved.
                LettuceClientConfiguration wrappedConfig = (LettuceClientConfiguration) Proxy.newProxyInstance(
                        LettuceClientConfiguration.class.getClassLoader(),
                        new Class<?>[]{LettuceClientConfiguration.class},
                        (proxy, method, args) -> {
                            if ("getClientResources".equals(method.getName())) {
                                return Optional.of(clientResources);
                            }
                            return method.invoke(originalConfig, args);
                        }
                );

                // Inject the wrapped config BEFORE Lettuce calls afterPropertiesSet()
                configField.set(factory, wrappedConfig);
                log.info("[OBSERVABILITY] Successfully injected tracing ClientResources into LettuceConnectionFactory '{}' BEFORE pool creation", beanName);

            } catch (NoSuchFieldException e) {
                log.error("[OBSERVABILITY] Could not find 'clientConfiguration' field in LettuceConnectionFactory '{}' — Redis spans may be missing. Spring Data Redis internal structure may have changed.", beanName, e);
            } catch (IllegalAccessException e) {
                log.error("[OBSERVABILITY] Could not access 'clientConfiguration' field in LettuceConnectionFactory '{}'", beanName, e);
            } catch (Exception e) {
                log.error("[OBSERVABILITY] Unexpected error injecting tracing into LettuceConnectionFactory '{}'", beanName, e);
            }
        }
        return bean;
    }
}
