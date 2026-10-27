package com.luxora.commerce.loyalty.dto;

import com.luxora.commerce.loyalty.model.LoyaltyTransactionType;
import java.time.Instant;
import java.util.UUID;

public record LoyaltyTransactionResponse(
        UUID id,
        UUID orderId,
        int points,
        LoyaltyTransactionType type,
        String description,
        Instant createdAt) {
}
