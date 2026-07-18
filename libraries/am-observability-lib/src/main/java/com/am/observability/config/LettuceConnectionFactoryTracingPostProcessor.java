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
 * is bypassed. This PostProcessor intercepts the factory bean after creation and programmatically
 * overrides its ClientResources configuration using a Dynamic Proxy to inject tracing.
 */
public class LettuceConnectionFactoryTracingPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(LettuceConnectionFactoryTracingPostProcessor.class);
    
    private final org.springframework.beans.factory.ObjectProvider<ClientResources> clientResourcesProvider;

    public LettuceConnectionFactoryTracingPostProcessor(org.springframework.beans.factory.ObjectProvider<ClientResources> clientResourcesProvider) {
        this.clientResourcesProvider = clientResourcesProvider;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof LettuceConnectionFactory factory) {
            try {
                log.info("[OBSERVABILITY] Intercepted LettuceConnectionFactory bean '{}' to inject tracing ClientResources programmatically", beanName);
                
                // Extract the original LettuceClientConfiguration field
                Field configField = LettuceConnectionFactory.class.getDeclaredField("clientConfiguration");
                configField.setAccessible(true);
                LettuceClientConfiguration originalConfig = (LettuceClientConfiguration) configField.get(factory);

                if (originalConfig == null) {
                    log.warn("[OBSERVABILITY] LettuceClientConfiguration was null in factory bean '{}', skipping tracing injection", beanName);
                    return bean;
                }

                ClientResources clientResources = clientResourcesProvider.getIfAvailable();
                if (clientResources == null) {
                    log.warn("[OBSERVABILITY] ClientResources bean not available yet, skipping tracing injection");
                    return bean;
                }

                // Check if it already uses our traced ClientResources
                Optional<ClientResources> currentResources = originalConfig.getClientResources();
                if (currentResources.isPresent() && currentResources.get() == clientResources) {
                    log.debug("[OBSERVABILITY] LettuceConnectionFactory '{}' is already configured with tracing", beanName);
                    return bean;
                }

                // Create a dynamic proxy that delegates all calls to the original configuration
                // EXCEPT getClientResources() which will return our traced ClientResources instance.
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

                // Set the modified configuration back into the factory
                configField.set(factory, wrappedConfig);
                log.info("[OBSERVABILITY] Successfully injected tracing ClientResources into LettuceConnectionFactory '{}'", beanName);

            } catch (NoSuchFieldException e) {
                log.error("[OBSERVABILITY] Failed to find clientConfiguration field in LettuceConnectionFactory to inject tracing", e);
            } catch (IllegalAccessException e) {
                log.error("[OBSERVABILITY] Failed to set clientConfiguration field in LettuceConnectionFactory to inject tracing", e);
            } catch (Exception e) {
                log.error("[OBSERVABILITY] Unexpected error injecting tracing into LettuceConnectionFactory '{}'", beanName, e);
            }
        }
        return bean;
    }
}
