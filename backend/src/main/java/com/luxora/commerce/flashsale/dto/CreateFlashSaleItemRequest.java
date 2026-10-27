package com.luxora.commerce.flashsale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateFlashSaleItemRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,

    UUID variantId,

    @NotNull(message = "Flash sale price is required")
    @Positive(message = "Flash sale price must be positive")
    BigDecimal flashSalePrice,

    @Min(value = 1, message = "Stock quantity must be at least 1")
    int stockQuantity
) {}
