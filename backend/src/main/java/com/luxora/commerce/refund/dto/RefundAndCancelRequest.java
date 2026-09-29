package com.luxora.commerce.refund.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefundAndCancelRequest(
        @Pattern(regexp = "SUCCEEDED|FAILED", message = "Mock refund outcome must be SUCCEEDED or FAILED")
        String mockOutcome,

        @Size(max = 500)
        String reason) {
}
