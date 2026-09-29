package com.luxora.commerce.auth.api;

import com.luxora.commerce.cart.service.CartService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthApiIntegrationTests {

    private static final String REFRESH_COOKIE_NAME = "luxora_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CartService cartService;

    @Test
    void register() throws Exception {
        AuthTokens tokens = registerUser("register@example.com");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshCookie()).isNotBlank();
        assertThat(tokens.body()).doesNotContain("refreshToken");
        assertThat(tokens.setCookie()).contains("HttpOnly", "SameSite=Lax", "Path=/api/v1/auth");
    }

    @Test
    void duplicateEmail() throws Exception {
        registerUser("duplicate@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "DUPLICATE@example.com",
                                "password", "Password123!",
                                "firstName", "Lux",
                                "lastName", "Ora"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void invalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "invalid-email",
                                "password", "Password123!",
                                "firstName", "Lux",
                                "lastName", "Ora"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void loginSuccessSetsRefreshCookie() throws Exception {
        registerUser("login@example.com");

        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "login@example.com", "password", "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains(REFRESH_COOKIE_NAME + "=", "HttpOnly", "SameSite=Lax");
    }

    @Test
    void loginWithAnonymousCartMergesCart() throws Exception {
        registerUser("merge-login@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Cart-Id", "anonymous-cart-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "merge-login@example.com", "password", "Password123!"))))
                .andExpect(status().isOk());

        verify(cartService).mergeAnonymousCartIntoUser(eq("anonymous-cart-id"), any());
    }

    @Test
    void wrongPassword() throws Exception {
        registerUser("wrong-password@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "wrong-password@example.com", "password", "bad-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void accessProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void accessProtectedEndpointWithValidToken() throws Exception {
        AuthTokens tokens = registerUser("me@example.com");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void refreshRotatesCookieAndDoesNotReturnRefreshToken() throws Exception {
        AuthTokens tokens = registerUser("refresh@example.com");
        AuthTokens rotated = refresh(tokens.refreshCookie());

        assertThat(rotated.accessToken()).isNotBlank();
        assertThat(rotated.refreshCookie()).isNotEqualTo(tokens.refreshCookie());
        assertThat(rotated.body()).doesNotContain("refreshToken");
    }

    @Test
    void oldRefreshTokenRejectedAfterRotation() throws Exception {
        AuthTokens tokens = registerUser("rotation@example.com");
        refresh(tokens.refreshCookie());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie(tokens.refreshCookie())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesTokenAndClearsCookie() throws Exception {
        AuthTokens tokens = registerUser("logout@example.com");

        var result = mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .cookie(refreshCookie(tokens.refreshCookie())))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains(REFRESH_COOKIE_NAME + "=", "Max-Age=0", "Path=/api/v1/auth");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie(tokens.refreshCookie())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void invalidRefreshRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie("not-a-valid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void expiredRefreshRejected() throws Exception {
        AuthTokens tokens = registerUser("expired-refresh@example.com");
        jdbcTemplate.update(
                "update refresh_tokens set expires_at = ? where token_hash = ?",
                java.time.Instant.parse("2000-01-01T00:00:00Z"),
                hash(tokens.refreshCookie()));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie(tokens.refreshCookie())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Lux",
                                "lastName", "Ora"))))
                .andExpect(status().isCreated())
                .andReturn();
        return tokensFrom(result.getResponse().getContentAsString(), result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    private AuthTokens refresh(String refreshToken) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        return tokensFrom(result.getResponse().getContentAsString(), result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    private AuthTokens tokensFrom(String responseBody, String setCookie) throws Exception {
        JsonNode json = objectMapper.readTree(responseBody);
        return new AuthTokens(json.get("accessToken").asText(), refreshCookieValue(setCookie), setCookie, responseBody);
    }

    private String refreshCookieValue(String setCookie) {
        assertThat(setCookie).isNotBlank();
        String prefix = REFRESH_COOKIE_NAME + "=";
        assertThat(setCookie).startsWith(prefix);
        return setCookie.substring(prefix.length(), setCookie.indexOf(';'));
    }

    private Cookie refreshCookie(String refreshToken) {
        return new Cookie(REFRESH_COOKIE_NAME, refreshToken);
    }

    private String hash(String token) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record AuthTokens(String accessToken, String refreshCookie, String setCookie, String body) {
    }
}