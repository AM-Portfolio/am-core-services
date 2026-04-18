package com.am.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Swagger/OpenAPI documentation in am-gateway.
 * Provides a clean interface for exploring AM Gateway capabilities.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AM Gateway Service API")
                        .version("1.0.0")
                        .description("WebSocket Gateway with Kafka Message Relay. " +
                                     "Handles real-time market data streaming and interest registration.")
                        .contact(new Contact()
                                .name("AM Platform Team")
                                .email("gateway-support@am-platform.com"))
                        .license(new License()
                                .name("Enterprise License")
                                .url("https://am-platform.com/license")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("am-gateway-public")
                .packagesToScan("com.am.gateway")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorApi() {
        return GroupedOpenApi.builder()
                .group("am-gateway-actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
