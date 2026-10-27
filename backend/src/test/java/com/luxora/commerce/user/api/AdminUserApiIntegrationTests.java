package com.luxora.commerce.user.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminUserApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listUsers_withoutAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_asAdmin_returnsUserList() throws Exception {
        String adminToken = loginAdmin();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").isNotEmpty())
                .andExpect(jsonPath("$[0].roles").isArray());
    }

    @Test
    void createUser_lock_unlock_andDelete_lifecycle() throws Exception {
        String adminToken = loginAdmin();
        String uniqueEmail = "client-" + UUID.randomUUID() + "@example.com";

        // 1. Create User
        var createResult = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", uniqueEmail,
                                "password", "Secret12345!",
                                "firstName", "Victoria",
                                "lastName", "Beckham",
                                "role", "ROLE_USER"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.firstName").value("Victoria"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();

        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Lock User (enabled = false)
        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.enabled").value(false));

        // Verify locked user cannot log in
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", uniqueEmail,
                                "password", "Secret12345!"))))
                .andExpect(status().isUnauthorized());

        // 3. Unlock User (enabled = true)
        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // Verify unlocked user can log in
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", uniqueEmail,
                                "password", "Secret12345!"))))
                .andExpect(status().isOk());

        // 4. Delete User
        mockMvc.perform(delete("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify deleted user cannot log in
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", uniqueEmail,
                                "password", "Secret12345!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotLockOrDeletePrimaryAdmin() throws Exception {
        String adminToken = loginAdmin();

        // Find primary admin id
        var listResult = mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        var users = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String adminId = null;
        for (var user : users) {
            if ("admin@luxora.com".equalsIgnoreCase(user.get("email").asText())) {
                adminId = user.get("id").asText();
                break;
            }
        }

        if (adminId != null) {
            // Cannot lock primary admin
            mockMvc.perform(patch("/api/v1/admin/users/" + adminId + "/status")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("enabled", false))))
                    .andExpect(status().isBadRequest());

            // Cannot delete primary admin
            mockMvc.perform(delete("/api/v1/admin/users/" + adminId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest());
        }
    }

    private String loginAdmin() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "admin@luxora.com",
                                "password", "Admin123456!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
