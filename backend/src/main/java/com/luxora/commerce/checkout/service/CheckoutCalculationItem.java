package com.luxora.commerce.checkout.service;

import com.luxora.commerce.catalog.model.ProductVariant;
import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutCalculationItem(
        ProductVariant variant,
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