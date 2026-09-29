package com.luxora.commerce.auth.dto;

import com.luxora.commerce.user.dto.CurrentUserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        CurrentUserResponse user) {
}