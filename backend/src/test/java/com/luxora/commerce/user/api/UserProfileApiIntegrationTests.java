package com.luxora.commerce.user.api;

import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserProfileApiIntegrationTests {

    private static final String REFRESH_COOKIE_NAME = "luxora_refresh_token";
    private static final String ORIGINAL_PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "NewPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void authenticatedGetCurrentUserWorks() throws Exception {
        AuthTokens tokens = registerUser("profile-get@example.com");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-get@example.com"))
                .andExpect(jsonPath("$.firstName").value("Lux"))
                .andExpect(jsonPath("$.lastName").value("Ora"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void authenticatedPatchUpdatesAllowedFieldsAndPersists() throws Exception {
        AuthTokens tokens = registerUser("profile-update@example.com");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("firstName", "Nova", "lastName", "Sterling"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-update@example.com"))
                .andExpect(jsonPath("$.firstName").value("Nova"))
                .andExpect(jsonPath("$.lastName").value("Sterling"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nova"))
                .andExpect(jsonPath("$.lastName").value("Sterling"));
    }

    @Test
    void invalidProfileValuesAreRejected() throws Exception {
        AuthTokens tokens = registerUser("profile-invalid@example.com");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("firstName", " ", "lastName", "Ora"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unauthenticatedPatchIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("firstName", "Nova", "lastName", "Sterling"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedFieldsCannotBeUpdated() throws Exception {
        AuthTokens tokens = registerUser("profile-protected@example.com");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "id", "00000000-0000-0000-0000-000000000000",
                                "email", "changed@example.com",
                                "passwordHash", "leaked",
                                "enabled", false,
                                "roles", java.util.List.of("ROLE_ADMIN"),
                                "firstName", "Safe",
                                "lastName", "Update"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile-protected@example.com"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.firstName").value("Safe"))
                .andExpect(jsonPath("$.lastName").value("Update"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void authenticatedPasswordChangeSucceedsAndRequiresLoginAgain() throws Exception {
        String email = "password-change@example.com";
        AuthTokens tokens = registerUser(email);
        String oldHash = passwordHash(email);

        var result = mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .cookie(refreshCookie(tokens.refreshCookie()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains(REFRESH_COOKIE_NAME + "=", "Max-Age=0", "Path=/api/v1/auth");
        String newHash = passwordHash(email);
        assertThat(newHash).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, newHash)).isTrue();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", ORIGINAL_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        AuthTokens newLogin = loginUser(email, NEW_PASSWORD);
        assertThat(newLogin.accessToken()).isNotBlank();
        assertThat(newLogin.refreshCookie()).isNotBlank();
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        AuthTokens tokens = registerUser("password-wrong-current@example.com");

        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("WrongPassword123!", NEW_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
    }

    @Test
    void mismatchedPasswordConfirmationIsRejected() throws Exception {
        AuthTokens tokens = registerUser("password-mismatch@example.com");

        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody(ORIGINAL_PASSWORD, NEW_PASSWORD, "Different123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_CONFIRMATION_MISMATCH"));
    }

    @Test
    void invalidNewPasswordIsRejected() throws Exception {
        AuthTokens tokens = registerUser("password-weak@example.com");

        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody(ORIGINAL_PASSWORD, "weakpass", "weakpass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void sameCurrentAndNewPasswordIsRejected() throws Exception {
        AuthTokens tokens = registerUser("password-same@example.com");

        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody(ORIGINAL_PASSWORD, ORIGINAL_PASSWORD, ORIGINAL_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));
    }

    @Test
    void unauthenticatedPasswordChangeIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void passwordChangeRevokesCurrentAndOtherRefreshTokens() throws Exception {
        String email = "password-revoke-sessions@example.com";
        AuthTokens firstSession = registerUser(email);
        AuthTokens secondSession = loginUser(email, ORIGINAL_PASSWORD);

        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + firstSession.accessToken())
                        .cookie(refreshCookie(firstSession.refreshCookie()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody(ORIGINAL_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie(firstSession.refreshCookie())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie(secondSession.refreshCookie())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", ORIGINAL_PASSWORD,
                                "firstName", "Lux",
                                "lastName", "Ora"))))
                .andExpect(status().isCreated())
                .andReturn();
        return tokensFrom(result.getResponse().getContentAsString(), result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    private AuthTokens loginUser(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return tokensFrom(result.getResponse().getContentAsString(), result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    private AuthTokens tokensFrom(String responseBody, String setCookie) throws Exception {
        JsonNode response = objectMapper.readTree(responseBody);
        assertThat(response.has("refreshToken")).isFalse();
        return new AuthTokens(response.get("accessToken").asText(), refreshCookieValue(setCookie), responseBody);
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

    private String passwordHash(String email) {
        return jdbcTemplate.queryForObject("select password_hash from users where email = ?", String.class, email);
    }

    private String changePasswordBody(String currentPassword, String newPassword, String confirmPassword) throws Exception {
        return json(Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword,
                "confirmPassword", confirmPassword));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record AuthTokens(String accessToken, String refreshCookie, String body) {
    }
}
