package com.luxora.commerce.banner.dto;

import java.time.Instant;
import java.util.UUID;

public record BannerResponse(
        UUID id,
        String title,
        String subtitle,
        String imageUrl,
        String linkUrl,
        String ctaText,
        int displayOrder,
        boolean active,
        Instant startsAt,
        Instant endsAt,
        Instant createdAt,
        Instant updatedAt
) {}
