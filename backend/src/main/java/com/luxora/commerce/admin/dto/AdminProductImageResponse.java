package com.luxora.commerce.admin.dto;

import java.util.UUID;

public record AdminProductImageResponse(
        UUID id,
        String url,
        String altText,
        int displayOrder) {
}