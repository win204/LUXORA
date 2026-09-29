package com.luxora.commerce.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminVariantCreateRequest(
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 80) String color,
        @Size(max = 80) String storage,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        boolean active,
        @Min(0) int quantityAvailable) {
}