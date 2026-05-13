package com.am.observability.config;

import com.am.observability.error.ObservabilityControllerAdvice;
import com.am.observability.feign.TraceContextFeignInterceptor;
import com.am.observability.flow.FlowLogger;
import com.am.observability.http.TraceContextSdkInterceptor;
import com.am.observability.kafka.TracingKafkaConsumerInterceptor;
import com.am.observability.kafka.TracingKafkaProducerInterceptor;
import com.am.observability.mdc.MdcTaskDecorator;
import com.am.observability.sanitize.Sanitizer;
import com.am.observability.stomp.StompTracingChannelInterceptor;
import com.am.observability.trace.TracingHelper;
import com.am.observability.web.RequestLoggingFilter;
import com.am.observability.web.TraceContextFilter;
import feign.RequestInterceptor;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Root auto-configuration for {@code am-observability-lib}. Activated by
 * Spring Boot via the {@code AutoConfiguration.imports} file. Backs out
 * entirely when {@code am.observability.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "am.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TracingHelper tracingHelper(ObjectProvider<Tracer> tracerProvider) {
        return new TracingHelper(tracerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public FlowLogger flowLogger(ObjectProvider<Tracer> tracerProvider,
                                  @Value("${spring.application.name:am-service}") String serviceName) {
        return new FlowLogger(tracerProvider.getIfAvailable(), serviceName);
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
        public RequestLoggingFilter requestLoggingFilter(ObservabilityProperties properties) {
            return new RequestLoggingFilter(properties.getRequestLog());
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
}
