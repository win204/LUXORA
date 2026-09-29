package com.luxora.commerce.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String status,
        BigDecimal grandTotal,
        String currency,
        int totalItems,
        List<OrderSummaryItemResponse> itemPreview,
        Instant createdAt) {
}