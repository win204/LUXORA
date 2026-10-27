package com.luxora.commerce.order.shipment.dto;

import java.time.Instant;

public record ShipmentResponse(
        String carrier,
        String trackingNumber,
        String trackingUrl,
        Instant shippedAt,
        Instant deliveredAt) {

    public ShipmentResponse(String carrier, String trackingNumber, Instant shippedAt, Instant deliveredAt) {
        this(carrier, trackingNumber, resolveTrackingUrl(carrier, trackingNumber), shippedAt, deliveredAt);
    }

    public static String resolveTrackingUrl(String carrier, String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        if ("GHN".equalsIgnoreCase(carrier) || "GIAOHANGNHANH".equalsIgnoreCase(carrier)) {
            return "https://donhang.ghn.vn/?order_code=" + trackingNumber.trim();
        }
        return null;
    }
}

