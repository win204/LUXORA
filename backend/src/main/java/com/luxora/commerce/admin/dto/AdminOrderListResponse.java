package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderListResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String status,
        BigDecimal grandTotal,
        String currency,
        int totalItems,
        String trackingNumber,
        Instant createdAt,
        Instant updatedAt) {
}