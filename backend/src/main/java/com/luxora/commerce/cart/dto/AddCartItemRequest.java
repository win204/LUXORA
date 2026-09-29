package com.luxora.commerce.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddCartItemRequest(
        String cartId,
        @NotNull UUID variantId,
        @Min(1) int quantity) {
}
