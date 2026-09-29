package com.luxora.commerce.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminProductImageRequest(
        @NotBlank @Size(max = 500) String url,
        @Size(max = 180) String altText,
        @Min(0) int displayOrder) {
}