package com.luxora.commerce.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminOrderStatusHistoryResponse(
        UUID id,
        String fromStatus,
        String toStatus,
        Instant changedAt,
        UUID changedByUserId,
        String changedByEmail) {
}