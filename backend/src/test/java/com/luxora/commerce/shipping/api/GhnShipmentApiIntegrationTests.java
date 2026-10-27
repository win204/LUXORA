package com.luxora.commerce.shipping.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.shipping.config.GhnProperties;
import com.luxora.commerce.shipping.provider.GhnShippingProvider;
import com.luxora.commerce.shipping.provider.ShippingOrderResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class GhnShipmentApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GhnProperties ghnProperties;

    @MockitoBean
    private CartStore cartStore;

    @MockitoBean
    private GhnShippingProvider ghnShippingProvider;

    private UUID variantId;
    private AuthTokens adminTokens;

    @BeforeEach
    void setUp() throws Exception {
        ProductVariant variant = variantRepository.findAll().stream()
                .filter(ProductVariant::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing active variant for integration tests"));
        variantId = variant.getId();

        String adminEmail = "ghn_admin_" + UUID.randomUUID().toString().substring(0, 8) + "@luxora.com";
        adminTokens = registerUser(adminEmail);
        promoteToAdmin(adminTokens.userId());
        adminTokens = login(adminEmail);
    }

    @Test
    @DisplayName("Admin creates shipment using GHN provider and receives GHN tracking number & URL")
    void adminCreateShipmentWithGhnProvider() throws Exception {
        String orderId = createProcessingOrder();

        when(ghnShippingProvider.createShippingOrder(any()))
                .thenReturn(new ShippingOrderResult(
                        "GHN",
                        "GHN-TRACK-9999",
                        "https://donhang.ghn.vn/?order_code=GHN-TRACK-9999",
                        BigDecimal.valueOf(25000),
                        Instant.now().plusSeconds(86400 * 3)));

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("provider", "GHN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shipment.carrier").value("GHN"))
                .andExpect(jsonPath("$.shipment.trackingNumber").value("GHN-TRACK-9999"))
                .andExpect(jsonPath("$.shipment.trackingUrl").value("https://donhang.ghn.vn/?order_code=GHN-TRACK-9999"));

        String dbStatus = jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, UUID.fromString(orderId));
        assertThat(dbStatus).isEqualTo("SHIPPED");
    }

    @Test
    @DisplayName("Admin creates shipment with manual carrier and tracking number successfully")
    void adminCreateShipmentWithManualCarrier() throws Exception {
        String orderId = createProcessingOrder();

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carrier", "VNPOST",
                                "trackingNumber", "VNPOST-123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shipment.carrier").value("VNPOST"))
                .andExpect(jsonPath("$.shipment.trackingNumber").value("VNPOST-123456"));
    }

    @Test
    @DisplayName("GHN webhook transitions order to DELIVERED and records deliveredAt timestamp")
    void ghnWebhookDeliveredSuccess() throws Exception {
        String orderId = createProcessingOrder();
        String trackingCode = "GHN-WH-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carrier", "GHN",
                                "trackingNumber", trackingCode))))
                .andExpect(status().isOk());

        String webhookPayload = """
                {
                    "OrderCode": "%s",
                    "Status": "delivered",
                    "Time": "2026-10-20T17:00:00Z"
                }
                """.formatted(trackingCode);

        mockMvc.perform(post("/api/v1/shipments/ghn-webhook")
                        .header("X-GHN-Token", ghnProperties.webhookToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));

        String dbStatus = jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, UUID.fromString(orderId));
        assertThat(dbStatus).isEqualTo("DELIVERED");

        Object deliveredAt = jdbcTemplate.queryForObject("select delivered_at from shipments where order_id = ?", Object.class, UUID.fromString(orderId));
        assertThat(deliveredAt).isNotNull();
    }

    @Test
    @DisplayName("GHN webhook returns 401 Unauthorized when token is invalid")
    void ghnWebhookInvalidTokenUnauthorized() throws Exception {
        String webhookPayload = """
                {
                    "OrderCode": "GHN-WH-FAKE",
                    "Status": "delivered"
                }
                """;

        mockMvc.perform(post("/api/v1/shipments/ghn-webhook")
                        .header("X-GHN-Token", "wrong-secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GHN_WEBHOOK_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GHN webhook is idempotent when called repeatedly on already delivered order")
    void ghnWebhookIdempotent() throws Exception {
        String orderId = createProcessingOrder();
        String trackingCode = "GHN-IDEMPOTENT-" + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carrier", "GHN",
                                "trackingNumber", trackingCode))))
                .andExpect(status().isOk());

        String webhookPayload = """
                {
                    "OrderCode": "%s",
                    "Status": "delivered"
                }
                """.formatted(trackingCode);

        // First call
        mockMvc.perform(post("/api/v1/shipments/ghn-webhook")
                        .header("X-GHN-Token", ghnProperties.webhookToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        // Second call (idempotent duplicate)
        mockMvc.perform(post("/api/v1/shipments/ghn-webhook")
                        .header("X-GHN-Token", ghnProperties.webhookToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));

        String dbStatus = jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, UUID.fromString(orderId));
        assertThat(dbStatus).isEqualTo("DELIVERED");
    }

    private String createProcessingOrder() throws Exception {
        when(cartStore.getQuantities("user:" + adminTokens.userId())).thenReturn(Map.of(variantId, 1));
        var created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + adminTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andReturn();
        String orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        jdbcTemplate.update("update orders set status = 'PROCESSING' where id = ?", UUID.fromString(orderId));
        return orderId;
    }

    private String validAddress() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "recipientName", "Nguyen Van B",
                "phone", "0901234567",
                "addressLine1", "123 Le Loi",
                "city", "District 1",
                "province", "Ho Chi Minh",
                "country", "Vietnam"));
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Admin",
                                "lastName", "GHN"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private AuthTokens login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private void promoteToAdmin(String userId) {
        UUID roleId;
        try {
            roleId = jdbcTemplate.queryForObject("select id from roles where name = ?", UUID.class, "ROLE_ADMIN");
        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            roleId = UUID.randomUUID();
            jdbcTemplate.update("insert into roles (id, name) values (?, ?)", roleId, "ROLE_ADMIN");
        }
        jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", UUID.fromString(userId), roleId);
    }

    private record AuthTokens(String accessToken, String userId) {
    }
}
