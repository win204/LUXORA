package com.luxora.commerce.shipping.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ShippingOrderRequest(
        UUID orderId,
        String recipientName,
        String recipientPhone,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String country,
        String postalCode,
        Integer weightGrams,
        BigDecimal codAmount,
        List<String> itemNames) {

    public ShippingOrderRequest {
        if (weightGrams == null || weightGrams <= 0) {
            weightGrams = 500;
        }
        if (codAmount == null) {
            codAmount = BigDecimal.ZERO;
        }
        if (itemNames == null) {
            itemNames = List.of();
        }
    }
}
