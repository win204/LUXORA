package com.luxora.commerce.returning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateReturnItemRequest(
        @NotNull UUID orderItemId,
        @Min(1) int quantity,
        @NotBlank @Size(max = 500) String reason) {
}
