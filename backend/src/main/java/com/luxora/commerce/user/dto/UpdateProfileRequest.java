package com.luxora.commerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 120, message = "First name must be 120 characters or fewer")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 120, message = "Last name must be 120 characters or fewer")
        String lastName) {
}