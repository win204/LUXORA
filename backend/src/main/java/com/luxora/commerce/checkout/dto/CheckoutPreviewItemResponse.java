package com.luxora.commerce.checkout.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutPreviewItemResponse(
        UUID productId,
        UUID variantId,
        String slug,
        String name,
        String sku,
        String color,
        String storage,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
