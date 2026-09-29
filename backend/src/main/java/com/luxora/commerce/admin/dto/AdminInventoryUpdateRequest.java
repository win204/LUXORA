package com.luxora.commerce.admin.dto;

import jakarta.validation.constraints.Min;

public record AdminInventoryUpdateRequest(
        @Min(0) int quantityAvailable) {
}