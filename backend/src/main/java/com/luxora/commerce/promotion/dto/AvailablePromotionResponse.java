package com.luxora.commerce.promotion.dto;

import com.luxora.commerce.promotion.model.PromotionType;
import java.math.BigDecimal;

public record AvailablePromotionResponse(
        String code,
        String name,
        String description,
        PromotionType type,
        BigDecimal value,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        BigDecimal potentialDiscount,
        boolean eligible,
        BigDecimal amountNeeded,
        boolean isBestDeal
) {}
