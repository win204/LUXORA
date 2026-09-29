package com.luxora.commerce.payment.dto;

import jakarta.validation.constraints.Pattern;

public record CreatePaymentRequest(
        @Pattern(regexp = "SUCCEEDED|FAILED", message = "Mock outcome must be SUCCEEDED or FAILED")
        String mockOutcome) {
}