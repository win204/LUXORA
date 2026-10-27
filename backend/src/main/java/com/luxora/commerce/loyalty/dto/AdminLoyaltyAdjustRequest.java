package com.luxora.commerce.loyalty.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminLoyaltyAdjustRequest(
        @NotNull(message = "Points value is required")
        Integer points,

        @NotBlank(message = "Adjustment reason is required")
        @Size(max = 255, message = "Reason cannot exceed 255 characters")
        String reason) {
}
