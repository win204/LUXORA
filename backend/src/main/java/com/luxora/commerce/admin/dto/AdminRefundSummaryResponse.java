package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminRefundSummaryResponse(
        UUID id,
        String provider,
        BigDecimal amount,
        String currency,
        String status,
        String reason,
        Instant createdAt,
        Instant updatedAt) {
}
