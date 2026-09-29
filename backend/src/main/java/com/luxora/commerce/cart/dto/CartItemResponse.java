package com.luxora.commerce.cart.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID itemId,
        UUID variantId,
        String productName,
        String productSlug,
        String sku,
        String color,
        String storage,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        boolean inStock) {
}
