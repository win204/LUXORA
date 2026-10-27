package com.luxora.commerce.order.shipment.dto;

import jakarta.validation.constraints.Size;

public record ShipmentRequest(
        @Size(max = 120)
        String carrier,

        @Size(max = 120)
        String trackingNumber,

        @Size(max = 50)
        String provider) {

    public ShipmentRequest(String carrier, String trackingNumber) {
        this(carrier, trackingNumber, null);
    }
}

