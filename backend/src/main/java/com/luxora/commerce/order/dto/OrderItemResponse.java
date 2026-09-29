package com.luxora.commerce.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String productSlug,
        String productName,
        String variantName,
        String sku,
        String color,
        String storage,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}