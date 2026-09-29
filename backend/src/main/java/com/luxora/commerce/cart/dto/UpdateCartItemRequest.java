package com.luxora.commerce.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        String cartId,
        @Min(1) int quantity) {
}
