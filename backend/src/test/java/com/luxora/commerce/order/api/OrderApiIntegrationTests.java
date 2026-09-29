package com.luxora.commerce.order.api;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OrderApiIntegrationTests {

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
        jdbcTemplate.update("update products set name = ? where slug = ?", "AeroPhone X1", "aerophone-x1");
        jdbcTemplate.update("update products set name = ? where slug = ?", "Studio One", "studio-one");
        jdbcTemplate.update("update product_variants set active = true");
        jdbcTemplate.update("update product_variants set price = ? where sku = ?", new BigDecimal("899.00"), "AUR-X1-GRF-128");
        jdbcTemplate.update("update product_variants set price = ? where sku = ?", new BigDecimal("999.00"), "AUR-X1-SLV-256");
        jdbcTemplate.update("update product_variants set price = ? where sku = ?", new BigDecimal("349.00"), "ATS-SO-MBK");
        resetStock("AUR-X1-GRF-128", 20);
        resetStock("AUR-X1-SLV-256", 8);
        resetStock("ATS-SO-MBK", 12);
    }

    @Test
    void authenticatedOrderCreationSucceeds() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-success@example.com");
        int stockBefore = stock(variant.getId());
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variant.getId(), 2));

        var result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].variantId").value(variant.getId().toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.shippingAddress.city").value("Ho Chi Minh City"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal expected = variant.getPrice().multiply(BigDecimal.valueOf(2));
        assertThat(body.get("subtotal").decimalValue()).isEqualByComparingTo(expected);
        assertThat(body.get("grandTotal").decimalValue()).isEqualByComparingTo(expected);
        assertThat(stock(variant.getId())).isEqualTo(stockBefore - 2);
        assertThat(orderCountFor(tokens.userId())).isEqualTo(1);
        assertThat(orderItemCount()).isEqualTo(1);
        verify(cartStore).clear(userKey(tokens));
    }

    @Test
    void unauthenticatedOrderRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void emptyCartRejectedAndCartPreserved() throws Exception {
        AuthTokens tokens = registerUser("order-empty@example.com");
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_CART"));

        assertThat(orderCountFor(tokens.userId())).isZero();
        verify(cartStore, never()).clear(userKey(tokens));
    }

    @Test
    void invalidShippingAddressRejected() throws Exception {
        AuthTokens tokens = registerUser("order-invalid-address@example.com");

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("recipientName", "", "phone", "bad", "country", "Vietnam"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void currentDatabasePriceIsUsedAndClientTotalsIgnored() throws Exception {
        ProductVariant variant = variant("ATS-SO-MBK");
        AuthTokens tokens = registerUser("order-authoritative-price@example.com");
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variant.getId(), 3));

        var result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "recipientName", "Lux Ora",
                                "phone", "+84901234567",
                                "addressLine1", "1 Dong Khoi",
                                "city", "Ho Chi Minh City",
                                "province", "Ho Chi Minh",
                                "country", "Vietnam",
                                "subtotal", "1.00",
                                "shippingFee", "9999.00",
                                "grandTotal", "1.00"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal expected = variant.getPrice().multiply(BigDecimal.valueOf(3));
        assertThat(body.get("subtotal").decimalValue()).isEqualByComparingTo(expected);
        assertThat(body.get("grandTotal").decimalValue()).isEqualByComparingTo(expected);
        assertThat(orderSubtotal(body.get("id").asText())).isEqualByComparingTo(expected);
    }

    @Test
    void insufficientStockRejectedWithoutSideEffects() throws Exception {
        ProductVariant variant = variant("AUR-X1-SLV-256");
        AuthTokens tokens = registerUser("order-insufficient-stock@example.com");
        int stockBefore = stock(variant.getId());
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variant.getId(), stockBefore + 1));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        assertThat(stock(variant.getId())).isEqualTo(stockBefore);
        assertThat(orderCountFor(tokens.userId())).isZero();
        verify(cartStore, never()).clear(userKey(tokens));
    }

    @Test
    void unavailableProductRejectedWithoutSideEffects() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-unavailable@example.com");
        int stockBefore = stock(variant.getId());
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variant.getId(), 1));
        jdbcTemplate.update("update product_variants set active = false where id = ?", variant.getId());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VARIANT_UNAVAILABLE"));

        assertThat(stock(variant.getId())).isEqualTo(stockBefore);
        assertThat(orderCountFor(tokens.userId())).isZero();
        verify(cartStore, never()).clear(userKey(tokens));
    }

    @Test
    void successfulOrderStoresSnapshots() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-snapshot@example.com");
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variant.getId(), 1));

        var created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        String orderId = createdBody.get("id").asText();
        BigDecimal originalPrice = createdBody.get("items").get(0).get("unitPrice").decimalValue();

        jdbcTemplate.update("update products set name = ? where id = ?", "Renamed Product", variant.getProduct().getId());
        jdbcTemplate.update("update product_variants set price = ? where id = ?", BigDecimal.ONE, variant.getId());

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("AeroPhone X1"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(originalPrice.doubleValue()));
    }

    @Test
    void failedOrderDoesNotPersistPartialRowsOrDecrementStock() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-rollback@example.com");
        int stockBefore = stock(variant.getId());
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        quantities.put(variant.getId(), 1);
        quantities.put(UUID.randomUUID(), 1);
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(quantities);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VARIANT_NOT_FOUND"));

        assertThat(stock(variant.getId())).isEqualTo(stockBefore);
        assertThat(orderCountFor(tokens.userId())).isZero();
        assertThat(orderItemCount()).isZero();
        verify(cartStore, never()).clear(userKey(tokens));
    }

    @Test
    void userCannotAccessAnotherUsersOrder() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("order-owner@example.com");
        AuthTokens other = registerUser("order-other@example.com");
        when(cartStore.getQuantities(userKey(owner))).thenReturn(Map.of(variant.getId(), 1));

        var created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andReturn();
        String orderId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void checkoutPreviewStillDoesNotChangeStock() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-preview-readonly@example.com");
        int stockBefore = stock(variant.getId());
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variant.getId(), 2));

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isOk());

        assertThat(stock(variant.getId())).isEqualTo(stockBefore);
        assertThat(orderCountFor(tokens.userId())).isZero();
        verify(cartStore, never()).clear(userKey(tokens));
    }


    @Test
    void emptyOrderHistoryReturnsEmptyPage() throws Exception {
        AuthTokens tokens = registerUser("order-history-empty@example.com");

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void orderHistoryReturnsOwnedOrdersNewestFirst() throws Exception {
        ProductVariant firstVariant = variant("AUR-X1-GRF-128");
        ProductVariant secondVariant = variant("ATS-SO-MBK");
        AuthTokens tokens = registerUser("order-history-owned@example.com");

        String firstOrderId = createOrder(tokens, Map.of(firstVariant.getId(), 1));
        Thread.sleep(10);
        String secondOrderId = createOrder(tokens, Map.of(secondVariant.getId(), 2));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(secondOrderId))
                .andExpect(jsonPath("$.content[0].totalItems").value(2))
                .andExpect(jsonPath("$.content[0].itemPreview[0].productName").value("Studio One"))
                .andExpect(jsonPath("$.content[1].id").value(firstOrderId))
                .andExpect(jsonPath("$.content[1].totalItems").value(1));
    }

    @Test
    void orderHistoryPaginationIsScopedToCurrentUser() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("order-history-page-owner@example.com");
        AuthTokens other = registerUser("order-history-page-other@example.com");

        createOrder(owner, Map.of(variant.getId(), 1));
        createOrder(owner, Map.of(variant.getId(), 1));
        createOrder(owner, Map.of(variant.getId(), 1));
        createOrder(other, Map.of(variant.getId(), 1));

        mockMvc.perform(get("/api/v1/orders?page=1&size=1")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void orderHistoryDoesNotExposeOtherUsersOrders() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("order-history-isolated-owner@example.com");
        AuthTokens other = registerUser("order-history-isolated-other@example.com");
        String ownerOrderId = createOrder(owner, Map.of(variant.getId(), 1));
        createOrder(other, Map.of(variant.getId(), 1));

        mockMvc.perform(get("/api/v1/orders?page=0&size=10")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownerOrderId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void unauthenticatedOrderHistoryRejected() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void orderDetailStillWorksForOwner() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-history-detail@example.com");
        String orderId = createOrder(tokens, Map.of(variant.getId(), 1));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.items.length()").value(1));
    }
    @Test
    void customerCancelsOwnPendingOrderAndRestoresInventory() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-cancel-owner@example.com");
        int stockBefore = stock(variant.getId());
        String orderId = createOrder(tokens, Map.of(variant.getId(), 2));
        assertThat(stock(variant.getId())).isEqualTo(stockBefore - 2);

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(stock(variant.getId())).isEqualTo(stockBefore);
        assertThat(historyCount(orderId)).isEqualTo(1);
    }

    @Test
    void customerCannotCancelAnotherUsersOrder() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("order-cancel-owner-only@example.com");
        AuthTokens other = registerUser("order-cancel-other@example.com");
        String orderId = createOrder(owner, Map.of(variant.getId(), 1));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void unauthenticatedCustomerCancelRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void repeatedCancelIsIdempotentAndDoesNotRestockTwice() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-cancel-idempotent@example.com");
        int stockBefore = stock(variant.getId());
        String orderId = createOrder(tokens, Map.of(variant.getId(), 1));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(stock(variant.getId())).isEqualTo(stockBefore);
        assertThat(historyCount(orderId)).isEqualTo(1);
    }

    @Test
    void nonPendingCustomerCancellationRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-cancel-status@example.com");
        for (String statusName : List.of("PAID", "PROCESSING", "SHIPPED", "DELIVERED")) {
            String orderId = createOrder(tokens, Map.of(variant.getId(), 1));
            markOrderStatus(orderId, statusName);

            mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                            .header("Authorization", "Bearer " + tokens.accessToken()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ORDER_CANCELLATION_INVALID"));
        }
    }

    @Test
    void paymentAfterCancellationRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-cancel-payment@example.com");
        String orderId = createOrder(tokens, Map.of(variant.getId(), 1));

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_CANCELLED"));
    }

    @Test
    void cancellationAndPaymentRaceDoesNotCorruptState() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("order-cancel-race@example.com");
        int stockBefore = stock(variant.getId());
        String orderId = createOrder(tokens, Map.of(variant.getId(), 1));
        int stockAfterOrder = stock(variant.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> cancel = () -> mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                            .header("Authorization", "Bearer " + tokens.accessToken()))
                    .andReturn().getResponse().getStatus();
            Callable<Integer> pay = () -> mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                            .header("Authorization", "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                    .andReturn().getResponse().getStatus();
            Future<Integer> cancelStatus = executor.submit(cancel);
            Future<Integer> payStatus = executor.submit(pay);
            cancelStatus.get();
            payStatus.get();
        } finally {
            executor.shutdownNow();
        }

        String finalStatus = orderStatus(orderId);
        if ("CANCELLED".equals(finalStatus)) {
            assertThat(stock(variant.getId())).isEqualTo(stockBefore);
            assertThat(successfulPaymentCount(orderId)).isZero();
            assertThat(historyCount(orderId)).isEqualTo(1);
        } else {
            assertThat(finalStatus).isEqualTo("PAID");
            assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder);
            assertThat(successfulPaymentCount(orderId)).isEqualTo(1);
            assertThat(historyCount(orderId)).isZero();
        }
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


    private String createOrder(AuthTokens tokens, Map<UUID, Integer> quantities) throws Exception {
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(quantities);
        var created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }    private ProductVariant variant(String sku) {
        return variantRepository.findAll().stream()
                .filter(variant -> sku.equals(variant.getSku()))
                .findFirst()
                .orElseThrow();
    }

    private void resetStock(String sku, int quantity) {
        jdbcTemplate.update("update inventory_items set quantity_available = ? where variant_id in (select id from product_variants where sku = ?)", quantity, sku);
    }

    private int stock(UUID variantId) {
        return jdbcTemplate.queryForObject("select quantity_available from inventory_items where variant_id = ?", Integer.class, variantId);
    }

    private int orderCountFor(String userId) {
        return jdbcTemplate.queryForObject("select count(*) from orders where user_id = ?", Integer.class, UUID.fromString(userId));
    }

    private int orderItemCount() {
        return jdbcTemplate.queryForObject("select count(*) from order_items", Integer.class);
    }


    private String orderStatus(String orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, UUID.fromString(orderId));
    }

    private void markOrderStatus(String orderId, String status) {
        jdbcTemplate.update("update orders set status = ? where id = ?", status, UUID.fromString(orderId));
    }

    private int historyCount(String orderId) {
        return jdbcTemplate.queryForObject("select count(*) from order_status_history where order_id = ?", Integer.class, UUID.fromString(orderId));
    }

    private int successfulPaymentCount(String orderId) {
        return jdbcTemplate.queryForObject("select count(*) from payments where order_id = ? and status = 'SUCCEEDED'", Integer.class, UUID.fromString(orderId));
    }
    private BigDecimal orderSubtotal(String orderId) {
        return jdbcTemplate.queryForObject("select subtotal from orders where id = ?", BigDecimal.class, UUID.fromString(orderId));
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
}





