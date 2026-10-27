package com.luxora.commerce.flashsale.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record FlashSaleResponse(
    UUID id,
    String name,
    String description,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    String status,
    String bannerImageUrl,
    long timeRemainingSeconds,
    List<FlashSaleItemResponse> items
) {}
