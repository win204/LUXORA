package com.luxora.commerce.loyalty.api;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.loyalty.dto.AdminLoyaltyAdjustRequest;
import com.luxora.commerce.loyalty.model.LoyaltyTier;
import com.luxora.commerce.loyalty.model.LoyaltyTransactionType;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LoyaltyApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CartStore cartStore;

    @BeforeEach
    void resetState() {
        reset(cartStore);
        jdbcTemplate.update("delete from loyalty_transactions");
        jdbcTemplate.update("delete from loyalty_accounts");
        jdbcTemplate.update("delete from return_status_history");
        jdbcTemplate.update("delete from refunds");
        jdbcTemplate.update("delete from return_items");
        jdbcTemplate.update("delete from returns");
        jdbcTemplate.update("delete from order_status_history");
        jdbcTemplate.update("delete from payments");
        jdbcTemplate.update("delete from shipments");
        jdbcTemplate.update("delete from order_items");
        jdbcTemplate.update("delete from orders");
    }

    @Test
    void getMyLoyaltyAccount_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/loyalty/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyLoyaltyAccount_newRegisteredUser_autoCreatesAccountWithZeroPointsAndSilverTier() throws Exception {
        AuthTokens tokens = registerUser("loyalty-newbie@example.com");

        mockMvc.perform(get("/api/v1/loyalty/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(0))
                .andExpect(jsonPath("$.lifetimePoints").value(0))
                .andExpect(jsonPath("$.tier").value(LoyaltyTier.SILVER.name()))
                .andExpect(jsonPath("$.nextTierPoints").value(500))
                .andExpect(jsonPath("$.pointsToNextTier").value(500))
                .andExpect(jsonPath("$.dollarValue").value(0.0));
    }

    @Test
    void adminAdjustPoints_awardsPoints_andUpgradesTier() throws Exception {
        AuthTokens customer = registerUser("loyalty-adjust@example.com");
        String adminToken = loginAdmin();

        // Add 600 points -> Should reach GOLD tier (threshold 500)
        AdminLoyaltyAdjustRequest request = new AdminLoyaltyAdjustRequest(600, "Bonus welcome points");
        mockMvc.perform(post("/api/v1/admin/loyalty/users/{userId}/adjust", customer.userId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(600))
                .andExpect(jsonPath("$.lifetimePoints").value(600))
                .andExpect(jsonPath("$.tier").value(LoyaltyTier.GOLD.name()))
                .andExpect(jsonPath("$.nextTierPoints").value(2000))
                .andExpect(jsonPath("$.pointsToNextTier").value(1400))
                .andExpect(jsonPath("$.dollarValue").value(6.0));

        // Verify customer sees updated balance and transaction history
        mockMvc.perform(get("/api/v1/loyalty/transactions")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value(LoyaltyTransactionType.ADMIN_ADJUSTED.name()))
                .andExpect(jsonPath("$.content[0].points").value(600))
                .andExpect(jsonPath("$.content[0].description").value("Bonus welcome points"));
    }

    @Test
    void adminAdjustPoints_nonAdmin_returnsForbidden() throws Exception {
        AuthTokens customer = registerUser("loyalty-customer1@example.com");
        AuthTokens customer2 = registerUser("loyalty-customer2@example.com");

        AdminLoyaltyAdjustRequest request = new AdminLoyaltyAdjustRequest(100, "Unauthorized adjust");
        mockMvc.perform(post("/api/v1/admin/loyalty/users/{userId}/adjust", customer2.userId())
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkoutPreview_withPointsRedemption_calculatesDiscountAndRemainingTotal() throws Exception {
        AuthTokens customer = registerUser("loyalty-checkout-preview@example.com");
        String adminToken = loginAdmin();

        // Admin adds 500 points ($5.00 discount)
        mockMvc.perform(post("/api/v1/admin/loyalty/users/{userId}/adjust", customer.userId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AdminLoyaltyAdjustRequest(500, "500 points"))))
                .andExpect(status().isOk());

        ProductVariant variant = variant("AUR-X1-GRF-128");
        when(cartStore.getQuantities("user:" + customer.userId())).thenReturn(Map.of(variant.getId(), 1));

        // Subtotal = $899.00. Redeem 300 points ($3.00)
        var previewRequest = Map.of(
                "recipientName", "Lux Customer",
                "phone", "+84901234567",
                "addressLine1", "1 Dong Khoi",
                "city", "Ho Chi Minh City",
                "province", "Ho Chi Minh",
                "country", "Vietnam",
                "postalCode", "700000",
                "redeemPoints", 300);

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(previewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(899.00))
                .andExpect(jsonPath("$.loyaltyPointsRedeemed").value(300))
                .andExpect(jsonPath("$.loyaltyDiscount").value(3.00))
                .andExpect(jsonPath("$.grandTotal").value(896.00));
    }

    @Test
    void checkoutPreview_redeemMoreThanBalance_isCappedToBalance() throws Exception {
        AuthTokens customer = registerUser("loyalty-cap-test@example.com");
        String adminToken = loginAdmin();

        // User only has 150 points ($1.50)
        mockMvc.perform(post("/api/v1/admin/loyalty/users/{userId}/adjust", customer.userId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AdminLoyaltyAdjustRequest(150, "150 points"))))
                .andExpect(status().isOk());

        ProductVariant variant = variant("AUR-X1-GRF-128");
        when(cartStore.getQuantities("user:" + customer.userId())).thenReturn(Map.of(variant.getId(), 1));

        // Request redeeming 500 points -> should be capped to user balance of 150 points
        var previewRequest = Map.of(
                "recipientName", "Lux Customer",
                "phone", "+84901234567",
                "addressLine1", "1 Dong Khoi",
                "city", "Ho Chi Minh City",
                "province", "Ho Chi Minh",
                "country", "Vietnam",
                "postalCode", "700000",
                "redeemPoints", 500);

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(previewRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyPointsRedeemed").value(150))
                .andExpect(jsonPath("$.loyaltyDiscount").value(1.50))
                .andExpect(jsonPath("$.grandTotal").value(897.50));
    }

    @Test
    void orderLifecycle_deductsPointsOnCreate_restoresOnCancel() throws Exception {
        AuthTokens customer = registerUser("loyalty-lifecycle@example.com");
        String adminToken = loginAdmin();

        // Credit 400 points ($4.00)
        mockMvc.perform(post("/api/v1/admin/loyalty/users/{userId}/adjust", customer.userId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AdminLoyaltyAdjustRequest(400, "Initial points"))))
                .andExpect(status().isOk());

        ProductVariant variant = variant("AUR-X1-GRF-128");
        when(cartStore.getQuantities("user:" + customer.userId())).thenReturn(Map.of(variant.getId(), 1));

        // Create order redeeming 200 points
        var orderRequest = Map.of(
                "recipientName", "Lux Customer",
                "phone", "+84901234567",
                "addressLine1", "1 Dong Khoi",
                "city", "Ho Chi Minh City",
                "province", "Ho Chi Minh",
                "country", "Vietnam",
                "postalCode", "700000",
                "redeemPoints", 200);

        var orderResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loyaltyPointsRedeemed").value(200))
                .andExpect(jsonPath("$.loyaltyDiscount").value(2.00))
                .andExpect(jsonPath("$.grandTotal").value(897.00))
                .andReturn();

        JsonNode orderNode = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        String orderId = orderNode.get("id").asText();

        // Customer's balance should now be 400 - 200 = 200 points
        mockMvc.perform(get("/api/v1/loyalty/me")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(200));

        // Cancel order -> points should be restored
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Customer balance should be restored back to 400 points
        mockMvc.perform(get("/api/v1/loyalty/me")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(400));
    }

    @Test
    void delivery_awardsLoyaltyPoints_basedOnTierMultiplier() throws Exception {
        AuthTokens customer = registerUser("loyalty-earn@example.com");
        String adminToken = loginAdmin();

        ProductVariant variant = variant("AUR-X1-GRF-128"); // $899.00
        when(cartStore.getQuantities("user:" + customer.userId())).thenReturn(Map.of(variant.getId(), 1));

        var orderRequest = Map.of(
                "recipientName", "Lux Customer",
                "phone", "+84901234567",
                "addressLine1", "1 Dong Khoi",
                "city", "Ho Chi Minh City",
                "province", "Ho Chi Minh",
                "country", "Vietnam",
                "postalCode", "700000");

        var orderResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asText();

        // Transition: PENDING -> PROCESSING -> SHIPPED -> DELIVERED
        jdbcTemplate.update("update orders set status = 'PROCESSING' where id = ?", UUID.fromString(orderId));

        // Create shipment
        var shipmentRequest = Map.of(
                "carrier", "GHN",
                "trackingNumber", "GHN-TEST-123456");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(shipmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        // Mark delivered -> Customer is SILVER (1x multiplier). Order grand total $899.00 -> 899 points earned!
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELIVERED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Verify customer account: 899 points awarded!
        // Lifetime points 899 >= 500 => automatically upgraded to GOLD!
        mockMvc.perform(get("/api/v1/loyalty/me")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(899))
                .andExpect(jsonPath("$.lifetimePoints").value(899))
                .andExpect(jsonPath("$.tier").value(LoyaltyTier.GOLD.name()));
    }

    private ProductVariant variant(String sku) {
        return variantRepository.findAll().stream()
                .filter(variant -> sku.equals(variant.getSku()))
                .findFirst()
                .orElseThrow();
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Loyalty",
                                "lastName", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(node.get("accessToken").asText(), node.get("user").get("id").asText());
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

    private record AuthTokens(String accessToken, String userId) {
    }
}
