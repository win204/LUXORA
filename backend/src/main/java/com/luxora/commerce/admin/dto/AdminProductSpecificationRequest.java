package com.luxora.commerce.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminProductSpecificationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String value,
        @Min(0) int displayOrder) {
}