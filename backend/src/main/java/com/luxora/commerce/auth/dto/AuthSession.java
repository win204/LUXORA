package com.luxora.commerce.auth.dto;

public record AuthSession(AuthResponse response, String refreshToken, long refreshTokenMaxAgeSeconds) {
}