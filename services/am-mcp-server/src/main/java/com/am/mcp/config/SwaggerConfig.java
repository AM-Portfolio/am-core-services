package com.am.mcp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Swagger/OpenAPI documentation.
 * Provides a clean interface for exploring AM Platform MCP Server capabilities.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AM Platform MCP Server API")
                        .version("1.0.0")
                        .description("Enterprise MCP server exposing AM Investment Platform tools to Claude. " +
                                     "Connects to internal services for Market Data, Trading, Portfolio and Analysis.")
                        .contact(new Contact()
                                .name("AM Platform Team")
                                .email("support@am-platform.com"))
                        .license(new License()
                                .name("Enterprise License")
                                .url("https://am-platform.com/license")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("am-mcp-public")
                .packagesToScan("com.am.mcp")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorApi() {
        return GroupedOpenApi.builder()
                .group("am-mcp-actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
