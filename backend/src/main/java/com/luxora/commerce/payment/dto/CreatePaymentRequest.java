package com.luxora.commerce.payment.dto;

import jakarta.validation.constraints.Pattern;

public record CreatePaymentRequest(
        @Pattern(regexp = "MOCK|VNPAY", message = "Provider must be MOCK or VNPAY")
        String provider,
        @Pattern(regexp = "SUCCEEDED|FAILED", message = "Mock outcome must be SUCCEEDED or FAILED")
        String mockOutcome,
        String bankCode,
        String returnUrl) {

    public CreatePaymentRequest(String mockOutcome) {
        this("MOCK", mockOutcome, null, null);
    }
}