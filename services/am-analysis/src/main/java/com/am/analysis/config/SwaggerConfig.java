package com.am.analysis.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for SPT / Swagger UI. Live doc: {@code /v3/api-docs}.
 */
@Configuration
public class SwaggerConfig {

    public static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI analysisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AM Analysis Service API")
                        .version("1.1.0")
                        .description("Dashboard and entity analysis APIs. "
                                + "SPT and agents build Try-it / load payloads from this OpenAPI document.")
                        .contact(new Contact()
                                .name("AM Core Services")
                                .email("core-services@am-platform.com"))
                        .license(new License()
                                .name("Enterprise License")
                                .url("https://am-platform.com/license")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Platform identity JWT (SPT uses try-token / identity login)")));
    }

    @Bean
    public GroupedOpenApi analysisApi() {
        return GroupedOpenApi.builder()
                .group("am-analysis")
                .packagesToScan("com.am.analysis.controller")
                .build();
    }
}
