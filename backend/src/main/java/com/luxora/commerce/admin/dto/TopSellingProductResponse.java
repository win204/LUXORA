package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopSellingProductResponse(
        UUID productId,
        String productName,
        String productSlug,
        String imageUrl,
        long totalQuantitySold,
        BigDecimal totalRevenue) {
}
