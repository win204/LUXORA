package com.luxora.commerce.auth.service;

import com.luxora.commerce.auth.dto.AuthResponse;
import com.luxora.commerce.auth.dto.AuthSession;
import com.luxora.commerce.auth.dto.LoginRequest;
import com.luxora.commerce.auth.dto.RegisterRequest;
import com.luxora.commerce.auth.model.RefreshToken;
import com.luxora.commerce.auth.repository.RefreshTokenRepository;
import com.luxora.commerce.auth.security.JwtService;
import com.luxora.commerce.cart.service.CartService;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.UnauthorizedException;
import com.luxora.commerce.user.model.Role;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.RoleRepository;
import com.luxora.commerce.user.repository.UserRepository;
import com.luxora.commerce.user.service.UserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CartService cartService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration refreshTokenTtl;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CartService cartService,
            @Value("${luxora.auth.refresh-token-ttl:PT168H}") Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cartService = cartService;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public AuthSession register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "Email is already registered");
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim());
        user.addRole(defaultUserRole());
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthSession login(LoginRequest request, String anonymousCartId) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> invalidCredentials());
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (StringUtils.hasText(anonymousCartId)) {
            cartService.mergeAnonymousCartIntoUser(anonymousCartId, user.getId());
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthSession refresh(String refreshToken) {
        String currentHash = hashToken(refreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(currentHash)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));
        if (!existing.isActive(Instant.now()) || !existing.getUser().isEnabled()) {
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }

        String nextRawToken = createRefreshTokenValue();
        String nextHash = hashToken(nextRawToken);
        existing.revoke(nextHash);
        RefreshToken replacement = new RefreshToken(existing.getUser(), nextHash, Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(replacement);

        return response(existing.getUser(), nextRawToken);
    }

    @Transactional
    public int revokeAllRefreshTokensForUser(User user) {
        return refreshTokenRepository.revokeAllActiveByUserId(user.getId(), Instant.now());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(null));
    }

    private AuthSession issueTokens(User user) {
        String refreshToken = createRefreshTokenValue();
        refreshTokenRepository.save(new RefreshToken(user, hashToken(refreshToken), Instant.now().plus(refreshTokenTtl)));
        return response(user, refreshToken);
    }

    private AuthSession response(User user, String refreshToken) {
        var roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail(), roles);
        AuthResponse response = new AuthResponse(
                accessToken,
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                UserMapper.toCurrentUser(user));
        return new AuthSession(response, refreshToken, refreshTokenTtl.toSeconds());
    }

    private Role defaultUserRole() {
        return roleRepository.findByName(ROLE_USER).orElseGet(() -> roleRepository.save(new Role(ROLE_USER)));
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("INVALID_EMAIL", "Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password");
    }

    private String createRefreshTokenValue() {
        byte[] value = new byte[48];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hashToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash refresh token", exception);
        }
    }
}
