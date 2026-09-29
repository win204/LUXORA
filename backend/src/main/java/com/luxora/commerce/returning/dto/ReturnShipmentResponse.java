package com.luxora.commerce.returning.dto;

import java.time.Instant;

public record ReturnShipmentResponse(
        String carrier,
        String trackingNumber,
        String mockLabelReference,
        Instant shippedAt,
        Instant receivedAt) {
}