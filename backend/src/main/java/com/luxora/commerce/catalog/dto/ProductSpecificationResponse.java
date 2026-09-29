package com.luxora.commerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Product specification")
public record ProductSpecificationResponse(UUID id, String name, String value, int displayOrder) {
}
