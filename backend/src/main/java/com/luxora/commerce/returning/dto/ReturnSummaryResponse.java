package com.luxora.commerce.returning.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReturnSummaryResponse(
        UUID id,
        UUID orderId,
        String userEmail,
        String status,
        int totalRequestedItems,
        int totalApprovedItems,
        int totalReceivedItems,
        BigDecimal estimatedRefund,
        String currency,
        String trackingNumber,
        Instant requestedAt,
        Instant updatedAt) {
}