package com.luxora.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LuxoraCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LuxoraCommerceApplication.class, args);
    }
}
