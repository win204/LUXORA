package com.luxora.commerce.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standard API error")
public record ApiErrorResponse(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp) {
}
