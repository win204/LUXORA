package com.luxora.commerce.catalog.repository;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminProductListMeta(
        UUID productId,
        long variantCount,
        BigDecimal minPrice,
        long inStockVariantCount) {
}