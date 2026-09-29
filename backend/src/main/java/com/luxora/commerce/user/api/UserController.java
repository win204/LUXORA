package com.luxora.commerce.user.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.user.dto.ChangePasswordRequest;
import com.luxora.commerce.user.dto.CurrentUserResponse;
import com.luxora.commerce.user.dto.PasswordChangedResponse;
import com.luxora.commerce.user.dto.UpdateProfileRequest;
import com.luxora.commerce.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private static final String REFRESH_COOKIE_NAME = "luxora_refresh_token";

    private final CurrentUserService currentUserService;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final String refreshCookiePath;

    public UserController(
            CurrentUserService currentUserService,
            @Value("${luxora.auth.refresh-cookie.secure:true}") boolean refreshCookieSecure,
            @Value("${luxora.auth.refresh-cookie.same-site:Lax}") String refreshCookieSameSite,
            @Value("${luxora.auth.refresh-cookie.path:/api/v1/auth}") String refreshCookiePath) {
        this.currentUserService = currentUserService;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshCookiePath = refreshCookiePath;
    }

    @GetMapping("/me")
    @Operation(summary = "Current user", description = "Returns the authenticated user profile.")
    @SecurityRequirement(name = "bearerAuth")
    CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return currentUserService.findCurrentUser(authenticatedUser);
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user", description = "Updates safe editable profile fields for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    CurrentUserResponse updateMe(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return currentUserService.updateCurrentUser(authenticatedUser, request);
    }

    @PostMapping("/me/password")
    @Operation(summary = "Change password", description = "Changes the authenticated user's password, revokes all refresh sessions, and clears the refresh cookie.")
    @SecurityRequirement(name = "bearerAuth")
    PasswordChangedResponse changePassword(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response) {
        PasswordChangedResponse result = currentUserService.changePassword(authenticatedUser, request);
        clearRefreshCookie(response);
        return result;
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
