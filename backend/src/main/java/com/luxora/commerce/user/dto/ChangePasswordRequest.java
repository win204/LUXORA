package com.luxora.commerce.user.dto;

import com.luxora.commerce.auth.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @StrongPassword String newPassword,
        @NotBlank String confirmPassword) {
}
