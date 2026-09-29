package com.luxora.commerce.returning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminReturnQuantityRequest(
        @NotNull UUID returnItemId,
        @Min(0) int quantity) {
}
