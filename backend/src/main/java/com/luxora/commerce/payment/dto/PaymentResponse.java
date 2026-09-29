package com.luxora.commerce.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String provider,
        String providerReference,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}