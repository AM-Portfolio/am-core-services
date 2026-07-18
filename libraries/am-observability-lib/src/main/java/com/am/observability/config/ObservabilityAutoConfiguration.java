package com.am.observability.config;

import com.am.observability.error.ObservabilityControllerAdvice;
import com.am.observability.feign.TraceContextFeignInterceptor;
import com.am.observability.flow.FlowLogger;
import com.am.observability.http.TraceContextSdkInterceptor;
import com.am.observability.kafka.TracingKafkaConsumerInterceptor;
import com.am.observability.kafka.TracingKafkaProducerInterceptor;
import com.am.observability.mdc.MdcTaskDecorator;
import com.am.observability.metrics.MetricNameMappingFilter;
import com.am.observability.sanitize.Sanitizer;
import com.am.observability.stomp.StompTracingChannelInterceptor;
import com.am.observability.trace.TracingHelper;
import com.am.observability.web.RequestLoggingFilter;
import com.am.observability.web.TraceContextFilter;
import feign.RequestInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.data.mongodb.observability.MongoObservationCommandListener;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Root auto-configuration for {@code am-observability-lib}. Activated by
 * Spring Boot via the {@code AutoConfiguration.imports} file. Backs out
 * entirely when {@code am.observability.enabled=false}.
 */
@AutoConfiguration(
    before = { RedisAutoConfiguration.class },
    after = {
        MongoAutoConfiguration.class,
        DataSourceAutoConfiguration.class
    }
)
@ConditionalOnProperty(prefix = "am.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    public ObservabilityAutoConfiguration() {
        try {
            ch.qos.logback.classic.Logger mongoLogger = (ch.qos.logback.classic.Logger)
                    org.slf4j.LoggerFactory.getLogger("org.springframework.data.mongodb.core.MongoTemplate");
            if (mongoLogger != null) {
                mongoLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
            }
        } catch (Throwable t) {
            // Guard against ClassNotFound or cast exception if a non-logback system is used
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public TracingHelper tracingHelper(ObjectProvider<Tracer> tracerProvider) {
        return new TracingHelper(tracerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowLogger flowLogger(ObjectProvider<Tracer> tracerProvider,
                                  ObjectProvider<ObservationRegistry> observationRegistryProvider,
                                  @Value("${spring.application.name:am-service}") String serviceName) {
        return new FlowLogger(
                tracerProvider.getIfAvailable(),
                observationRegistryProvider.getIfAvailable(() -> ObservationRegistry.NOOP),
                serviceName);
    }

    @Bean
    @ConditionalOnMissingBean
    public Sanitizer sanitizer(ObservabilityProperties properties) {
        Set<String> extra = new LinkedHashSet<>(properties.getSanitize().getExtraFields());
        return new Sanitizer(extra, properties.getSanitize().getPreviewBytes());
    }

    @Bean
    @ConditionalOnMissingBean(name = "amObservabilityTaskDecorator")
    public TaskDecorator amObservabilityTaskDecorator() {
        return new MdcTaskDecorator();
    }

    /**
     * Remaps local Micrometer meter names to canonical concept names used by
     * shared Grafana domain tiles. Spring Boot applies all {@link MeterFilter}
     * beans to the managed {@code MeterRegistry}. Empty map is a no-op.
     */
    @Bean
    @ConditionalOnMissingBean(name = "amObservabilityMetricNameMappingFilter")
    @ConditionalOnClass(MeterFilter.class)
    public MeterFilter amObservabilityMetricNameMappingFilter(ObservabilityProperties properties) {
        return new MetricNameMappingFilter(properties.getMetrics().getMap());
    }

    /**
     * Adds the {@code application} common tag required by Grafana Service
     * discovery ({@code label_values(jvm_memory_used_bytes, application)}).
     * Matches portfolio-app's {@code management.metrics.tags.application}.
     */
    @Bean
    @ConditionalOnMissingBean(name = "amObservabilityApplicationTagCustomizer")
    @ConditionalOnClass(MeterRegistry.class)
    public MeterRegistryCustomizer<MeterRegistry> amObservabilityApplicationTagCustomizer(
            @Value("${spring.application.name:am-service}") String serviceName) {
        return registry -> registry.config().commonTags("application", serviceName);
    }

    // ---------------- Web ----------------

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    static class WebConfig {

        @Bean
        @ConditionalOnMissingBean
        public TraceContextFilter traceContextFilter(TracingHelper tracingHelper,
                                                     @Value("${spring.application.name:am-service}") String serviceName) {
            return new TraceContextFilter(tracingHelper, serviceName);
        }

        @Bean
        public FilterRegistrationBean<TraceContextFilter> traceContextFilterRegistration(TraceContextFilter filter) {
            FilterRegistrationBean<TraceContextFilter> bean = new FilterRegistrationBean<>(filter);
            bean.setOrder(TraceContextFilter.ORDER);
            return bean;
        }

        @Bean
        @ConditionalOnProperty(prefix = "am.observability.request-log", name = "enabled", havingValue = "true", matchIfMissing = true)
        @ConditionalOnMissingBean
        public RequestLoggingFilter requestLoggingFilter(ObservabilityProperties properties, Sanitizer sanitizer) {
            return new RequestLoggingFilter(properties.getRequestLog(), sanitizer);
        }

        @Bean
        @ConditionalOnProperty(prefix = "am.observability.request-log", name = "enabled", havingValue = "true", matchIfMissing = true)
        public FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration(RequestLoggingFilter filter) {
            FilterRegistrationBean<RequestLoggingFilter> bean = new FilterRegistrationBean<>(filter);
            bean.setOrder(RequestLoggingFilter.ORDER);
            return bean;
        }

        @Bean
        @ConditionalOnMissingBean
        public ObservabilityControllerAdvice observabilityControllerAdvice() {
            return new ObservabilityControllerAdvice();
        }
    }

    // ---------------- Feign ----------------

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(feign.RequestInterceptor.class)
    static class FeignConfig {

        @Bean(name = "traceContextFeignInterceptor")
        public RequestInterceptor traceContextFeignInterceptor() {
            return new TraceContextFeignInterceptor();
        }
    }

    // ---------------- Kafka ----------------

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.kafka.clients.producer.ProducerInterceptor")
    static class KafkaConfig {

        @Bean
        @ConditionalOnMissingBean
        public TracingKafkaProducerInterceptor amObservabilityKafkaProducerInterceptor() {
            return new TracingKafkaProducerInterceptor();
        }

        @Bean
        @ConditionalOnMissingBean
        public TracingKafkaConsumerInterceptor amObservabilityKafkaConsumerInterceptor() {
            return new TracingKafkaConsumerInterceptor();
        }
    }

    // ---------------- STOMP ----------------

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.messaging.simp.stomp.StompHeaderAccessor")
    static class StompConfig {

        @Bean
        @ConditionalOnMissingBean
        public StompTracingChannelInterceptor stompTracingChannelInterceptor(TracingHelper tracingHelper,
                                                                              @Value("${spring.application.name:am-service}") String serviceName) {
            return new StompTracingChannelInterceptor(tracingHelper, serviceName);
        }
    }

    // ---------------- SDK ----------------

    @Bean
    @ConditionalOnMissingBean
    public TraceContextSdkInterceptor traceContextSdkInterceptor(TracingHelper tracingHelper) {
        return new TraceContextSdkInterceptor(tracingHelper);
    }

    // ---------------- Observed & AOP Auto-Tracing ----------------

    /**
     * [WHY THIS WAS ADDED]:
     * Creates the ObservedAspect bean to make the `@Observed` annotation work.
     * With this, developers can manually label specific methods for detailed timing.
     */
    @Bean
    @ConditionalOnClass(ObservedAspect.class)
    @ConditionalOnMissingBean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

    /**
     * [WHY THIS WAS ADDED]:
     * Automatically traces all methods in `@Service` classes without needing annotations.
     * Uses @ConditionalOnProperty so it can be disabled in application.yml if needed.
     */
    @Bean
    @ConditionalOnClass(org.aspectj.lang.ProceedingJoinPoint.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "am.observability.service-tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
    public AutoObservabilityAspect autoObservabilityAspect(ObservationRegistry observationRegistry) {
        return new AutoObservabilityAspect(observationRegistry);
    }

    // ---------------- MongoDB Tracing ----------------

    /**
     * [WHY THIS WAS ADDED]:
     * Automatically hooks into the MongoDB driver to record the execution time and raw queries.
     * Activated ONLY if MongoDB client libraries are on the microservice's classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({com.mongodb.client.MongoClient.class, MongoClientSettingsBuilderCustomizer.class})
    static class MongoTracingConfig {

        @Bean
        @ConditionalOnMissingBean
        public MongoClientSettingsBuilderCustomizer mongoObservabilityCustomizer(ObservationRegistry observationRegistry) {
            return builder -> builder.addCommandListener(
                new MongoObservationCommandListener(observationRegistry)
            );
        }
    }

    // ---------------- Lettuce Redis Tracing ----------------

    /**
     * [WHY THIS WAS ADDED]:
     * Automatically hooks into the Lettuce Redis driver to record cache queries (e.g. GET, SET, TTL).
     * Activated ONLY if the Lettuce Redis client libraries are present in the classpath.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({io.lettuce.core.resource.ClientResources.class, io.lettuce.core.tracing.MicrometerTracing.class})
    static class LettuceTracingConfig {

        @Bean(destroyMethod = "shutdown")
        @ConditionalOnMissingBean(io.lettuce.core.resource.ClientResources.class)
        public io.lettuce.core.resource.ClientResources lettuceClientResources(ObservationRegistry observationRegistry) {
            return io.lettuce.core.resource.DefaultClientResources.builder()
                    .tracing(new io.lettuce.core.tracing.MicrometerTracing(observationRegistry, "redis", true))
                    .build();
        }

        @Bean
        public LettuceConnectionFactoryTracingPostProcessor lettuceConnectionFactoryTracingPostProcessor(
                io.lettuce.core.resource.ClientResources clientResources) {
            return new LettuceConnectionFactoryTracingPostProcessor(clientResources);
        }
    }
}
