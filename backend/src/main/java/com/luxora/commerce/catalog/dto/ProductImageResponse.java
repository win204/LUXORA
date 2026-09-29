package com.luxora.commerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Product image")
public record ProductImageResponse(UUID id, String url, String altText, int displayOrder) {
}
