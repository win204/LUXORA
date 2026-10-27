package com.luxora.commerce.loyalty.dto;

import com.luxora.commerce.loyalty.model.LoyaltyTier;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoyaltyAccountResponse(
        UUID id,
        UUID userId,
        int currentPoints,
        BigDecimal dollarValue,
        int lifetimePoints,
        LoyaltyTier tier,
        double pointMultiplier,
        boolean freeShipping,
        int nextTierPoints,
        int pointsToNextTier,
        Instant updatedAt) {
}
