package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminProductVariantResponse(
        UUID id,
        String sku,
        String color,
        String storage,
        BigDecimal price,
        boolean active,
        boolean inStock,
        int quantityAvailable) {
}