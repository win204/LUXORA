package com.luxora.commerce.shipping.provider;

import java.math.BigDecimal;
import java.time.Instant;

public record ShippingOrderResult(
        String carrier,
        String trackingNumber,
        String trackingUrl,
        BigDecimal fee,
        Instant expectedDeliveryDate) {
}
