package com.luxora.commerce.common.config;

import com.luxora.commerce.common.api.ApiErrorResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI luxoraOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .info(new Info()
                        .title("LUXORA Commerce API")
                        .version("v1")
                        .description("Catalog and authentication API for the LUXORA modular monolith."));
    }

    @Bean
    OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
            openApi.components(components.addSchemas("ApiErrorResponse", new Schema<ApiErrorResponse>()
                    .name("ApiErrorResponse")
                    .description("Standard API error")));
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                operation.getResponses().addApiResponse("400", errorResponse("Bad request"));
                operation.getResponses().addApiResponse("401", errorResponse("Authentication required or invalid"));
                operation.getResponses().addApiResponse("404", errorResponse("Resource not found"));
            }));
        };
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        "application/json",
                        new MediaType().schema(new Schema<ApiErrorResponse>().$ref("#/components/schemas/ApiErrorResponse"))));
    }
}
