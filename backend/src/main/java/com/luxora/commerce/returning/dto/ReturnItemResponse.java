package com.luxora.commerce.returning.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReturnItemResponse(
        UUID id,
        UUID orderItemId,
        String productName,
        String sku,
        BigDecimal unitPrice,
        int purchasedQuantity,
        int requestedQuantity,
        int approvedQuantity,
        int receivedQuantity,
        String reason) {
}
