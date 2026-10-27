package com.luxora.commerce.aws.ses;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AwsSesEmailNotificationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testEmailEndpoint_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/emails/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "toEmail", "test@luxora.com",
                                "type", "GENERIC"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testEmailEndpoint_customer_returnsForbidden() throws Exception {
        String customerToken = registerCustomer("ses-client@luxora.com");

        mockMvc.perform(post("/api/v1/admin/emails/test")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "toEmail", "test@luxora.com",
                                "type", "GENERIC"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testEmailEndpoint_admin_generic_returnsSuccess() throws Exception {
        String adminToken = loginAdmin();

        mockMvc.perform(post("/api/v1/admin/emails/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "toEmail", "client-vip@luxora.com",
                                "type", "GENERIC",
                                "subject", "SES System Ping"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.toEmail").value("client-vip@luxora.com"))
                .andExpect(jsonPath("$.type").value("GENERIC"));
    }

    @Test
    void testEmailEndpoint_admin_shipment_returnsSuccess() throws Exception {
        String adminToken = loginAdmin();

        mockMvc.perform(post("/api/v1/admin/emails/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "toEmail", "client-vip@luxora.com",
                                "type", "SHIPMENT"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.toEmail").value("client-vip@luxora.com"))
                .andExpect(jsonPath("$.type").value("SHIPMENT"));
    }

    @Test
    void testEmailEndpoint_admin_tierUpgrade_returnsSuccess() throws Exception {
        String adminToken = loginAdmin();

        mockMvc.perform(post("/api/v1/admin/emails/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "toEmail", "client-vip@luxora.com",
                                "type", "TIER_UPGRADE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.toEmail").value("client-vip@luxora.com"))
                .andExpect(jsonPath("$.type").value("TIER_UPGRADE"));
    }

    @Test
    void testEmailEndpoint_admin_orderConfirmation_returnsSuccess() throws Exception {
        String adminToken = loginAdmin();

        mockMvc.perform(post("/api/v1/admin/emails/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "toEmail", "client-vip@luxora.com",
                                "type", "ORDER_CONFIRMATION"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.toEmail").value("client-vip@luxora.com"))
                .andExpect(jsonPath("$.type").value("ORDER_CONFIRMATION"));
    }

    @Test
    void testEmailEndpoint_admin_invalidEmail_returnsBadRequest() throws Exception {
        String adminToken = loginAdmin();

        mockMvc.perform(post("/api/v1/admin/emails/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "toEmail", "not-a-valid-email",
                                "type", "GENERIC"
                        ))))
                .andExpect(status().isBadRequest());
    }

    private String registerCustomer(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Luxury",
                                "lastName", "Client"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
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
