package com.luxora.commerce.returning.dto;

import java.time.Instant;
import java.util.UUID;

public record ReturnStatusHistoryResponse(
        UUID id,
        String fromStatus,
        String toStatus,
        Instant changedAt,
        UUID changedByUserId,
        String changedByEmail) {
}
