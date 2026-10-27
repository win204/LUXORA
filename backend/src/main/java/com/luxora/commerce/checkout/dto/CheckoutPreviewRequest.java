package com.luxora.commerce.checkout.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CheckoutPreviewRequest(
        @Size(max = 254) @Email(message = "Invalid email address") String email,
        @NotBlank @Size(max = 160) String recipientName,
        @NotBlank @Size(max = 32) @Pattern(regexp = "^[+0-9() .-]{7,32}$", message = "Phone number is invalid") String phone,
        @NotBlank @Size(max = 240) String addressLine1,
        @Size(max = 240) String addressLine2,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 120) String province,
        @NotBlank @Size(max = 120) String country,
        @Size(max = 24) String postalCode,
        @Size(max = 64) String promotionCode,
        @Min(value = 0, message = "Redeem points cannot be negative") Integer redeemPoints,
        Boolean autoApplyBestVoucher) {

    public CheckoutPreviewRequest(
            String email,
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String province,
            String country,
            String postalCode,
            String promotionCode,
            Integer redeemPoints) {
        this(email, recipientName, phone, addressLine1, addressLine2, city, province, country, postalCode, promotionCode, redeemPoints, false);
    }

    public CheckoutPreviewRequest(
            String email,
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String province,
            String country,
            String postalCode,
            String promotionCode) {
        this(email, recipientName, phone, addressLine1, addressLine2, city, province, country, postalCode, promotionCode, null, false);
    }

    public CheckoutPreviewRequest(
            String recipientName,
            String phone,
            String addressLine1,
            String addressLine2,
            String city,
            String province,
            String country,
            String postalCode,
            String promotionCode) {
        this(null, recipientName, phone, addressLine1, addressLine2, city, province, country, postalCode, promotionCode, null);
    }
}