package com.luxora.commerce.banner.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record BannerUpsertRequest(
        @NotBlank(message = "Title is required")
        String title,
        String subtitle,
        @NotBlank(message = "Image URL is required")
        String imageUrl,
        String linkUrl,
        String ctaText,
        Integer displayOrder,
        Boolean active,
        Instant startsAt,
        Instant endsAt
) {}
