package com.luxora.commerce.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminPaymentSummaryResponse(
        UUID id,
        String provider,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}