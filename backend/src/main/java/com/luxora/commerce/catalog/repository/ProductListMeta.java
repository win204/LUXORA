package com.luxora.commerce.catalog.repository;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductListMeta(UUID productId, BigDecimal minPrice, long availableVariants) {

    public boolean inStock() {
        return availableVariants > 0;
    }
}
