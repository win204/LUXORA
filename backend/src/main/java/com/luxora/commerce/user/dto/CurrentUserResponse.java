package com.luxora.commerce.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt) {
}
