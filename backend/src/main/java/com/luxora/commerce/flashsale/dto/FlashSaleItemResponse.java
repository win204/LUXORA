package com.luxora.commerce.flashsale.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FlashSaleItemResponse(
    UUID id,
    UUID productId,
    String productName,
    String productSlug,
    String productImageUrl,
    BigDecimal originalPrice,
    UUID variantId,
    String variantName,
    BigDecimal flashSalePrice,
    int discountPercent,
    int stockQuantity,
    int soldQuantity,
    int percentClaimed,
    int availableQuantity
) {}
