package com.luxora.commerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product list item")
public record ProductListResponse(
        UUID id,
        String name,
        String slug,
        String subtitle,
        BrandResponse brand,
        CategoryResponse category,
        BigDecimal minPrice,
        boolean inStock) {
}
