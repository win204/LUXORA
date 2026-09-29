package com.luxora.commerce.auth.dto;

import com.luxora.commerce.auth.validation.StrongPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @StrongPassword String password,
        @NotBlank @Size(max = 120) String firstName,
        @NotBlank @Size(max = 120) String lastName) {
}

