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

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
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
        return new org.springframework.kafka.core.KafkaTemplate<>(producerFactory());
    }
}
