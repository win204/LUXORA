package com.luxora.commerce.common.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    CorsConfig(@Value("${luxora.cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Accept", "Content-Type", "Authorization", "X-Cart-Id")
                .allowCredentials(true)
                .exposedHeaders("X-Cart-Id")
                .maxAge(3600);

        registry.addMapping("/v3/api-docs/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("Accept", "Content-Type")
                .maxAge(3600);

        registry.addMapping("/actuator/health/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("Accept")
                .maxAge(3600);
    }
}
