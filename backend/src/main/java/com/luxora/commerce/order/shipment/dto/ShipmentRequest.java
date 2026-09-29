package com.luxora.commerce.order.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipmentRequest(
        @NotBlank @Size(max = 120)
        String carrier,

        @NotBlank @Size(max = 120)
        String trackingNumber) {
}
