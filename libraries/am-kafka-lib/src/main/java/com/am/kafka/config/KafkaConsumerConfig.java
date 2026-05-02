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
        
        // Ensure security properties are correctly mapped if provided via spring.kafka.properties
        Map<String, String> commonProps = kafkaProperties.getProperties();
        if (commonProps.containsKey("security-protocol")) {
            props.put("security.protocol", commonProps.get("security-protocol"));
        }
        if (commonProps.containsKey("sasl-mechanism")) {
            props.put("sasl.mechanism", commonProps.get("sasl-mechanism"));
        }
        if (commonProps.containsKey("sasl-jaas-config")) {
            props.put("sasl.jaas.config", commonProps.get("sasl-jaas-config"));
        }

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new DefaultKafkaConsumerFactory<>(props);
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
        if (commonProps.containsKey("security-protocol")) {
            props.put("security.protocol", commonProps.get("security-protocol"));
        }
        if (commonProps.containsKey("sasl-mechanism")) {
            props.put("sasl.mechanism", commonProps.get("sasl-mechanism"));
        }
        if (commonProps.containsKey("sasl-jaas-config")) {
            props.put("sasl.jaas.config", commonProps.get("sasl-jaas-config"));
        }

        return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate() {
        return new org.springframework.kafka.core.KafkaTemplate<>(producerFactory());
    }
}
