package com.luxora.commerce.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Product detail")
public record ProductDetailResponse(
        UUID id,
        String name,
        String slug,
        String subtitle,
        String description,
        BrandResponse brand,
        CategoryResponse category,
        List<ProductImageResponse> images,
        List<ProductSpecificationResponse> specifications,
        List<ProductVariantResponse> variants) {
}
