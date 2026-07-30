package com.am.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "am.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:am-default-group}")
    private String groupId;

    private final org.springframework.boot.autoconfigure.kafka.KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(org.springframework.boot.autoconfigure.kafka.KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        // Explicit overrides if @Value is provided and different from properties
        if (bootstrapServers != null && !bootstrapServers.equals("localhost:9092")) {
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        }
        if (groupId != null && !groupId.equals("am-default-group")) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }
        
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Robustly map security properties from kebab-case (Spring) to dot-notation (Kafka)
        Map<String, String> commonProps = kafkaProperties.getProperties();
        mapSecurityProperty(commonProps, "security-protocol", "security.protocol", props);
        mapSecurityProperty(commonProps, "security.protocol", "security.protocol", props);
        mapSecurityProperty(commonProps, "sasl-mechanism", "sasl.mechanism", props);
        mapSecurityProperty(commonProps, "sasl.mechanism", "sasl.mechanism", props);
        mapSecurityProperty(commonProps, "sasl-jaas-config", "sasl.jaas.config", props);
        mapSecurityProperty(commonProps, "sasl.jaas.config", "sasl.jaas.config", props);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private void mapSecurityProperty(Map<String, String> source, String sourceKey, String targetKey, Map<String, Object> target) {
        if (source.containsKey(sourceKey)) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Emit a consumer span per received record so Kafka hops appear in Tempo
        // and are tied to the producing trace via propagated headers.
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

    @Bean
    public org.springframework.kafka.core.ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        
        // Ensure security properties are correctly mapped if provided via spring.kafka.properties
        Map<String, String> commonProps = kafkaProperties.getProperties();
        mapSecurityProperty(commonProps, "security-protocol", "security.protocol", props);
        mapSecurityProperty(commonProps, "security.protocol", "security.protocol", props);
        mapSecurityProperty(commonProps, "sasl-mechanism", "sasl.mechanism", props);
        mapSecurityProperty(commonProps, "sasl.mechanism", "sasl.mechanism", props);
        mapSecurityProperty(commonProps, "sasl-jaas-config", "sasl.jaas.config", props);
        mapSecurityProperty(commonProps, "sasl.jaas.config", "sasl.jaas.config", props);

        return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate() {
        org.springframework.kafka.core.KafkaTemplate<String, String> template =
                new org.springframework.kafka.core.KafkaTemplate<>(producerFactory());
        // Emit a producer span per send; the ObservationRegistry bean is picked
        // up from the application context automatically.
        template.setObservationEnabled(true);
        return template;
    }

    /**
     * Configures an explicit KafkaAdmin bean with properly mapped SASL security configuration properties.
     * <p>
     * Why this is necessary:
     * When Spring Boot's Micrometer Observation tracing is enabled (template.setObservationEnabled(true)),
     * Spring Kafka queries KafkaAdmin.clusterId() before publishing messages. If KafkaAdmin is auto-configured
     * without mapping kebab-case Spring properties ("security-protocol") to native Apache Kafka dot-notation
     * ("security.protocol"), native Kafka AdminClient falls back to unauthenticated PLAINTEXT, resulting in a
     * 60-second TimeoutException and blocking event publication.
     *
     * @return Fully configured KafkaAdmin instance ready for authenticated SASL operations.
     */
    @Bean
    public org.springframework.kafka.core.KafkaAdmin kafkaAdmin() {
        // Build base admin configuration map from Spring Boot auto-configured KafkaProperties
        Map<String, Object> kafkaAdminConfigurationProperties = new HashMap<>(kafkaProperties.buildAdminProperties());
        
        // Retrieve raw common properties supplied under spring.kafka.properties
        Map<String, String> rawSpringCommonProperties = kafkaProperties.getProperties();
        
        // Robustly map security properties from both kebab-case (Spring convention) and dot-notation (Kafka native)
        mapSecurityProperty(rawSpringCommonProperties, "security-protocol", "security.protocol", kafkaAdminConfigurationProperties);
        mapSecurityProperty(rawSpringCommonProperties, "security.protocol", "security.protocol", kafkaAdminConfigurationProperties);
        mapSecurityProperty(rawSpringCommonProperties, "sasl-mechanism", "sasl.mechanism", kafkaAdminConfigurationProperties);
        mapSecurityProperty(rawSpringCommonProperties, "sasl.mechanism", "sasl.mechanism", kafkaAdminConfigurationProperties);
        mapSecurityProperty(rawSpringCommonProperties, "sasl-jaas-config", "sasl.jaas.config", kafkaAdminConfigurationProperties);
        mapSecurityProperty(rawSpringCommonProperties, "sasl.jaas.config", "sasl.jaas.config", kafkaAdminConfigurationProperties);

        return new org.springframework.kafka.core.KafkaAdmin(kafkaAdminConfigurationProperties);
    }
}

