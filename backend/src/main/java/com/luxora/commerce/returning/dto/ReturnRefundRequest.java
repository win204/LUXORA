package com.luxora.commerce.returning.dto;

import jakarta.validation.constraints.Size;

public record ReturnRefundRequest(
        @Size(max = 40) String mockOutcome,
        @Size(max = 500) String reason) {
}
