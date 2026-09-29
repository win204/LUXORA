package com.luxora.commerce.returning.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReturnResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        String userEmail,
        String status,
        String customerNote,
        String adminNote,
        String currency,
        BigDecimal estimatedRefund,
        BigDecimal receivedRefundAmount,
        Instant requestedAt,
        Instant approvedAt,
        Instant rejectedAt,
        Instant receivedAt,
        Instant refundedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt,
        ReturnShipmentResponse shipment,
        List<ReturnItemResponse> items,
        List<ReturnStatusHistoryResponse> statusHistory) {
}
