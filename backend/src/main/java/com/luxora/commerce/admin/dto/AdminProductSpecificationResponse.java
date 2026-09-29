package com.luxora.commerce.admin.dto;

import java.util.UUID;

public record AdminProductSpecificationResponse(
        UUID id,
        String name,
        String value,
        int displayOrder) {
}