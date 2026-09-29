package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminProductListResponse(
        UUID id,
        String name,
        String slug,
        String brandName,
        String categoryName,
        boolean active,
        int variantCount,
        BigDecimal minPrice,
        boolean inStock) {
}