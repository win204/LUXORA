package com.luxora.commerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product variant")
public record ProductVariantResponse(
        UUID id,
        String sku,
        String color,
        String storage,
        BigDecimal price,
        boolean inStock) {
}
