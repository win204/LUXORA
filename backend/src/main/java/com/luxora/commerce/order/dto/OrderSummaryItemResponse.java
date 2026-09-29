package com.luxora.commerce.order.dto;

import java.util.UUID;

public record OrderSummaryItemResponse(
        UUID productId,
        UUID variantId,
        String productName,
        String variantName,
        String sku,
        int quantity) {
}