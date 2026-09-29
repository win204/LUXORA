package com.luxora.commerce.payment.api;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PaymentApiIntegrationTests {

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
        jdbcTemplate.update("delete from return_status_history");
        jdbcTemplate.update("delete from refunds");
        jdbcTemplate.update("delete from return_items");
        jdbcTemplate.update("delete from returns");
        jdbcTemplate.update("delete from order_status_history");
        jdbcTemplate.update("delete from payments");
        jdbcTemplate.update("delete from shipments");
        jdbcTemplate.update("delete from order_items");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("update products set active = true");
        jdbcTemplate.update("update product_variants set active = true");
        jdbcTemplate.update("update product_variants set price = ? where sku = ?", new BigDecimal("899.00"), "AUR-X1-GRF-128");
        resetStock("AUR-X1-GRF-128", 20);
    }

    @Test
    void paymentSuccessMarksPaymentAndOrderPaid() throws Exception {
        ProductVariant variant = variant();
        AuthTokens tokens = registerUser("payment-success@example.com");
        CreatedOrder order = createOrder(tokens, variant, 1);
        int stockAfterOrder = stock(variant.getId());

        var paymentResult = mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.provider").value("MOCK"))
                .andExpect(jsonPath("$.orderId").value(order.id()))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        JsonNode payment = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
        assertThat(payment.get("amount").decimalValue()).isEqualByComparingTo(order.grandTotal());
        assertThat(orderStatus(order.id())).isEqualTo("PAID");
        assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder);

        mockMvc.perform(get("/api/v1/orders/{id}", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void paymentFailureLeavesOrderPendingAndAllowsRetry() throws Exception {
        ProductVariant variant = variant();
        AuthTokens tokens = registerUser("payment-failure@example.com");
        CreatedOrder order = createOrder(tokens, variant, 1);

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "FAILED"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));
        assertThat(orderStatus(order.id())).isEqualTo("PENDING");

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        assertThat(orderStatus(order.id())).isEqualTo("PAID");
    }

    @Test
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/payments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void ownershipRejected() throws Exception {
        ProductVariant variant = variant();
        AuthTokens owner = registerUser("payment-owner@example.com");
        AuthTokens other = registerUser("payment-other@example.com");
        CreatedOrder order = createOrder(owner, variant, 1);

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + other.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void serverUsesPersistedOrderTotalAndIgnoresClientAmountCurrency() throws Exception {
        ProductVariant variant = variant();
        AuthTokens tokens = registerUser("payment-authoritative@example.com");
        CreatedOrder order = createOrder(tokens, variant, 2);

        var result = mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "mockOutcome", "SUCCEEDED",
                                "amount", "1.00",
                                "currency", "VND"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        JsonNode payment = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(payment.get("amount").decimalValue()).isEqualByComparingTo(order.grandTotal());
        assertThat(paymentAmount(payment.get("id").asText())).isEqualByComparingTo(order.grandTotal());
    }

    @Test
    void alreadyPaidOrderCannotBePaidAgain() throws Exception {
        ProductVariant variant = variant();
        AuthTokens tokens = registerUser("payment-double@example.com");
        CreatedOrder order = createOrder(tokens, variant, 1);

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_ALREADY_PAID"));
    }

    @Test
    void cancelledOrderCannotBePaid() throws Exception {
        ProductVariant variant = variant();
        AuthTokens tokens = registerUser("payment-cancelled@example.com");
        CreatedOrder order = createOrder(tokens, variant, 1);
        jdbcTemplate.update("update orders set status = 'CANCELLED' where id = ?", UUID.fromString(order.id()));

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_CANCELLED"));
    }

    @Test
    void latestPaymentReturnsLatestAttempt() throws Exception {
        ProductVariant variant = variant();
        AuthTokens tokens = registerUser("payment-latest@example.com");
        CreatedOrder order = createOrder(tokens, variant, 1);

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "FAILED"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders/{id}/payments/latest", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void paymentDoesNotChangeInventoryOrSnapshots() throws Exception {
        ProductVariant variant = variant();
        AuthTokens tokens = registerUser("payment-readonly-stock@example.com");
        CreatedOrder order = createOrder(tokens, variant, 1);
        int stockAfterOrder = stock(variant.getId());
        String itemNameBefore = orderItemName(order.id());
        BigDecimal itemPriceBefore = orderItemPrice(order.id());

        jdbcTemplate.update("update products set name = ? where id = ?", "Renamed Product", variant.getProduct().getId());
        jdbcTemplate.update("update product_variants set price = ? where id = ?", BigDecimal.ONE, variant.getId());

        mockMvc.perform(post("/api/v1/orders/{id}/payments", order.id())
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isCreated());

        assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder);
        assertThat(orderItemName(order.id())).isEqualTo(itemNameBefore);
        assertThat(orderItemPrice(order.id())).isEqualByComparingTo(itemPriceBefore);
    }

    private CreatedOrder createOrder(AuthTokens tokens, ProductVariant variant, int quantity) throws Exception {
        when(cartStore.getQuantities(eq(userKey(tokens)))).thenReturn(Map.of(variant.getId(), quantity));
        var result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CreatedOrder(body.get("id").asText(), body.get("grandTotal").decimalValue());
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
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private ProductVariant variant() {
        return variantRepository.findAll().stream()
                .filter(variant -> "AUR-X1-GRF-128".equals(variant.getSku()))
                .findFirst()
                .orElseThrow();
    }

    private void resetStock(String sku, int quantity) {
        jdbcTemplate.update("update inventory_items set quantity_available = ? where variant_id in (select id from product_variants where sku = ?)", quantity, sku);
    }

    private int stock(UUID variantId) {
        return jdbcTemplate.queryForObject("select quantity_available from inventory_items where variant_id = ?", Integer.class, variantId);
    }

    private String orderStatus(String orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, UUID.fromString(orderId));
    }

    private BigDecimal paymentAmount(String paymentId) {
        return jdbcTemplate.queryForObject("select amount from payments where id = ?", BigDecimal.class, UUID.fromString(paymentId));
    }

    private String orderItemName(String orderId) {
        return jdbcTemplate.queryForObject("select top 1 product_name from order_items where order_id = ?", String.class, UUID.fromString(orderId));
    }

    private BigDecimal orderItemPrice(String orderId) {
        return jdbcTemplate.queryForObject("select top 1 unit_price from order_items where order_id = ?", BigDecimal.class, UUID.fromString(orderId));
    }

    private String userKey(AuthTokens tokens) {
        return "user:" + tokens.userId();
    }

    private String validAddress() throws Exception {
        return json(Map.of(
                "recipientName", "Lux Ora",
                "phone", "+84901234567",
                "addressLine1", "1 Dong Khoi",
                "addressLine2", "Suite 8",
                "city", "Ho Chi Minh City",
                "province", "Ho Chi Minh",
                "country", "Vietnam",
                "postalCode", "700000"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private record AuthTokens(String accessToken, String userId) {
    }

    private record CreatedOrder(String id, BigDecimal grandTotal) {
    }
}





