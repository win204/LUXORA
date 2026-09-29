package com.luxora.commerce.order.shipment.dto;

import java.time.Instant;

public record ShipmentResponse(
        String carrier,
        String trackingNumber,
        Instant shippedAt,
        Instant deliveredAt) {
}
