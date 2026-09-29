package com.luxora.commerce.auth.api;

import com.luxora.commerce.auth.dto.AuthResponse;
import com.luxora.commerce.auth.dto.AuthSession;
import com.luxora.commerce.auth.dto.LoginRequest;
import com.luxora.commerce.auth.dto.RegisterRequest;
import com.luxora.commerce.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "luxora_refresh_token";

    private final AuthService authService;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final String refreshCookiePath;

    public AuthController(
            AuthService authService,
            @Value("${luxora.auth.refresh-cookie.secure:true}") boolean refreshCookieSecure,
            @Value("${luxora.auth.refresh-cookie.same-site:Lax}") String refreshCookieSameSite,
            @Value("${luxora.auth.refresh-cookie.path:/api/v1/auth}") String refreshCookiePath) {
        this.authService = authService;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshCookiePath = refreshCookiePath;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register", description = "Creates a user account, sets a refresh cookie, and returns an access token.")
    AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthSession session = authService.register(request);
        setRefreshCookie(response, session);
        return session.response();
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates with email and password, sets a refresh cookie, and merges X-Cart-Id when supplied.")
    AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(name = "X-Cart-Id", required = false) String anonymousCartId,
            HttpServletResponse response) {
        AuthSession session = authService.login(request, anonymousCartId);
        setRefreshCookie(response, session);
        return session.response();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Reads the refresh cookie, rotates it, and returns a new access token.")
    AuthResponse refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthSession session = authService.refresh(refreshToken);
        setRefreshCookie(response, session);
        return session.response();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout", description = "Invalidates the refresh cookie token and clears the cookie.")
    void logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
    }

    private void setRefreshCookie(HttpServletResponse response, AuthSession session) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, session.refreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(Duration.ofSeconds(session.refreshTokenMaxAgeSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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