package com.luxora.commerce.returning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRejectReturnRequest(
        @NotBlank @Size(max = 500) String adminNote) {
}
