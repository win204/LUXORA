package com.luxora.commerce.checkout.dto;

public record ShippingAddressResponse(
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String country,
        String postalCode) {
}
