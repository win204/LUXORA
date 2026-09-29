package com.luxora.commerce.admin.dto;

import java.util.List;
import java.util.UUID;

public record AdminProductDetailResponse(
        UUID id,
        String name,
        String slug,
        String subtitle,
        String description,
        UUID brandId,
        String brandName,
        UUID categoryId,
        String categoryName,
        boolean active,
        List<AdminProductImageResponse> images,
        List<AdminProductSpecificationResponse> specifications,
        List<AdminProductVariantResponse> variants) {
}