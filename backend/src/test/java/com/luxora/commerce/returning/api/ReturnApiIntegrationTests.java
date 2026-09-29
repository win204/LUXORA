package com.luxora.commerce.returning.api;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
class ReturnApiIntegrationTests {

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
        jdbcTemplate.update("delete from return_shipments");
        jdbcTemplate.update("delete from returns");
        jdbcTemplate.update("delete from order_status_history");
        jdbcTemplate.update("delete from payments");
        jdbcTemplate.update("delete from shipments");
        jdbcTemplate.update("delete from order_items");
        jdbcTemplate.update("delete from orders");
        jdbcTemplate.update("update products set active = true");
        jdbcTemplate.update("update product_variants set active = true");
        resetStock("AUR-X1-GRF-128", 20);
        ensureAdminRole();
    }

    @Test
    void onlyDeliveredOrdersWithDeliveryTimestampAreEligible() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("return-eligible-owner@example.com");
        String orderId = createOrder(owner, variant.getId(), 1);
        String orderItemId = orderItemId(orderId);

        mockMvc.perform(post("/api/v1/orders/{orderId}/returns", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload(orderItemId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_ORDER_NOT_ELIGIBLE"));
    }

    @Test
    void ownershipAndAuthenticationAreEnforced() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-auth-admin@example.com");
        AuthTokens owner = registerUser("return-auth-owner@example.com");
        AuthTokens other = registerUser("return-auth-other@example.com");
        String orderId = deliveredOrder(admin, owner, variant.getId(), 1);
        String orderItemId = orderItemId(orderId);

        mockMvc.perform(post("/api/v1/orders/{orderId}/returns", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload(orderItemId, 1)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/orders/{orderId}/returns", orderId)
                        .header("Authorization", "Bearer " + other.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload(orderItemId, 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void expiredReturnWindowRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-window-admin@example.com");
        AuthTokens owner = registerUser("return-window-owner@example.com");
        String orderId = deliveredOrder(admin, owner, variant.getId(), 1);
        jdbcTemplate.update("update shipments set delivered_at = ? where order_id = ?", OffsetDateTime.now().minusDays(30), UUID.fromString(orderId));

        mockMvc.perform(post("/api/v1/orders/{orderId}/returns", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload(orderItemId(orderId), 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_WINDOW_EXPIRED"));
    }

    @Test
    void invalidQuantityAndOverReturnAreRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-quantity-admin@example.com");
        AuthTokens owner = registerUser("return-quantity-owner@example.com");
        String orderId = deliveredOrder(admin, owner, variant.getId(), 1);
        String orderItemId = orderItemId(orderId);

        mockMvc.perform(post("/api/v1/orders/{orderId}/returns", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload(orderItemId, 0)))
                .andExpect(status().isBadRequest());

        createReturn(owner, orderId, orderItemId, 1);
        mockMvc.perform(post("/api/v1/orders/{orderId}/returns", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload(orderItemId, 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_QUANTITY_EXCEEDED"));
    }

    @Test
    void partialReturnApproveReceiveRefundLifecycle() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-lifecycle-admin@example.com");
        AuthTokens owner = registerUser("return-lifecycle-owner@example.com");
        int stockBefore = stock(variant.getId());
        String orderId = deliveredOrder(admin, owner, variant.getId(), 2);
        int stockAfterOrder = stock(variant.getId());
        String returnId = createReturn(owner, orderId, orderItemId(orderId), 1);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/approve", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "Approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].approvedQuantity").value(1));
        assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder);
        generateLabel(admin, returnId);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/mark-received", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "Received"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].receivedQuantity").value(1));
        int stockAfterReceive = stock(variant.getId());
        assertThat(stockAfterReceive).isEqualTo(stockAfterOrder + 1);
        assertThat(stockAfterReceive).isEqualTo(stockBefore - 1);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/mark-received", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "Again"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        assertThat(stock(variant.getId())).isEqualTo(stockAfterReceive);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/refund", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "FAILED", "reason", "Mock failure"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        mockMvc.perform(post("/api/v1/admin/returns/{id}/refund", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED", "reason", "Return refund"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.receivedRefundAmount").value(unitPrice(orderId).doubleValue()));

        mockMvc.perform(post("/api/v1/admin/returns/{id}/refund", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
        assertThat(successfulReturnRefundCount(returnId)).isEqualTo(1);
        assertThat(orderStatus(orderId)).isEqualTo("DELIVERED");
        assertThat(returnHistoryCount(returnId)).isEqualTo(4);
    }

    @Test
    void rejectAndCustomerCancelWork() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-reject-admin@example.com");
        AuthTokens owner = registerUser("return-reject-owner@example.com");
        String rejectedOrderId = deliveredOrder(admin, owner, variant.getId(), 1);
        String rejectedReturnId = createReturn(owner, rejectedOrderId, orderItemId(rejectedOrderId), 1);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/reject", rejectedReturnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "Not eligible"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        String cancelledOrderId = deliveredOrder(admin, owner, variant.getId(), 1);
        String cancelledReturnId = createReturn(owner, cancelledOrderId, orderItemId(cancelledOrderId), 1);
        mockMvc.perform(post("/api/v1/returns/{id}/cancel", cancelledReturnId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminAuthorizationEnforced() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-admin-auth-admin@example.com");
        AuthTokens owner = registerUser("return-admin-auth-owner@example.com");
        AuthTokens normal = registerUser("return-admin-auth-normal@example.com");
        String orderId = deliveredOrder(admin, owner, variant.getId(), 1);
        String returnId = createReturn(owner, orderId, orderItemId(orderId), 1);

        mockMvc.perform(get("/api/v1/admin/returns/{id}", returnId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/returns/{id}", returnId)
                        .header("Authorization", "Bearer " + normal.accessToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/returns/{id}", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(returnId));
    }


    @Test
    void shippingLabelAndMarkShippedAreVisibleAndIdempotent() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-label-admin@example.com");
        AuthTokens owner = registerUser("return-label-owner@example.com");
        AuthTokens other = registerUser("return-label-other@example.com");
        AuthTokens normal = registerUser("return-label-normal@example.com");
        String orderId = deliveredOrder(admin, owner, variant.getId(), 1);
        String returnId = createReturn(owner, orderId, orderItemId(orderId), 1);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/shipping-label", returnId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/admin/returns/{id}/shipping-label", returnId)
                        .header("Authorization", "Bearer " + normal.accessToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/returns/{id}/shipping-label", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_LABEL_INVALID"));

        approveReturn(admin, returnId);
        String tracking = objectMapper.readTree(mockMvc.perform(post("/api/v1/admin/returns/{id}/shipping-label", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment.carrier").value("LUXORA Mock Returns"))
                .andExpect(jsonPath("$.shipment.trackingNumber").exists())
                .andExpect(jsonPath("$.shipment.mockLabelReference").exists())
                .andReturn().getResponse().getContentAsString()).get("shipment").get("trackingNumber").asText();

        mockMvc.perform(post("/api/v1/admin/returns/{id}/shipping-label", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment.trackingNumber").value(tracking));
        assertThat(returnShipmentCount(returnId)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/returns/{id}", returnId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment.trackingNumber").value(tracking));
        mockMvc.perform(get("/api/v1/returns/{id}", returnId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/returns/{id}/mark-shipped", returnId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment.shippedAt").exists());
        String shippedAt = returnShippedAt(returnId);
        mockMvc.perform(post("/api/v1/returns/{id}/mark-shipped", returnId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment.shippedAt").exists());
        assertThat(returnShippedAt(returnId)).isEqualTo(shippedAt);
    }

    @Test
    void receiveRequiresReturnShipmentAndSetsReceivedAt() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-receive-shipping-admin@example.com");
        AuthTokens owner = registerUser("return-receive-shipping-owner@example.com");
        int stockBefore = stock(variant.getId());
        String orderId = deliveredOrder(admin, owner, variant.getId(), 1);
        String returnId = createReturn(owner, orderId, orderItemId(orderId), 1);
        approveReturn(admin, returnId);
        int stockAfterOrder = stock(variant.getId());

        mockMvc.perform(post("/api/v1/admin/returns/{id}/mark-received", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "No shipment"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RETURN_SHIPMENT_REQUIRED"));
        assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder);

        generateLabel(admin, returnId);
        mockMvc.perform(post("/api/v1/admin/returns/{id}/mark-received", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "Received"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.shipment.receivedAt").exists());
        int stockAfterReceive = stock(variant.getId());
        assertThat(stockAfterReceive).isEqualTo(stockBefore);

        mockMvc.perform(post("/api/v1/admin/returns/{id}/mark-received", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "Again"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
        assertThat(stock(variant.getId())).isEqualTo(stockAfterReceive);
    }
    @Test
    void adminQueueSupportsFiltersPaginationSortingAndOperationalNotes() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("return-queue-admin@example.com");
        AuthTokens normal = registerUser("return-queue-normal@example.com");
        AuthTokens alice = registerUser("return-queue-alice@example.com");
        AuthTokens bob = registerUser("return-queue-bob@example.com");

        String aliceOrderId = deliveredOrder(admin, alice, variant.getId(), 1);
        String requestedReturnId = createReturn(alice, aliceOrderId, orderItemId(aliceOrderId), 1);
        String bobOrderId = deliveredOrder(admin, bob, variant.getId(), 1);
        String approvedReturnId = createReturn(bob, bobOrderId, orderItemId(bobOrderId), 1);
        approveReturn(admin, approvedReturnId);
        generateLabel(admin, approvedReturnId);
        String tracking = returnTracking(approvedReturnId);

        OffsetDateTime oldest = OffsetDateTime.now().minusDays(2);
        OffsetDateTime newest = OffsetDateTime.now().minusHours(1);
        jdbcTemplate.update("update returns set requested_at = ? where id = ?", oldest, UUID.fromString(requestedReturnId));
        jdbcTemplate.update("update returns set requested_at = ? where id = ?", newest, UUID.fromString(approvedReturnId));

        mockMvc.perform(get("/api/v1/admin/returns"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + normal.accessToken()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .param("status", "REQUESTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(requestedReturnId))
                .andExpect(jsonPath("$.content[0].userEmail").value("return-queue-alice@example.com"));

        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .param("customerEmail", "queue-bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(approvedReturnId));

        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .param("orderId", aliceOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(requestedReturnId));

        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .param("trackingNumber", tracking.substring(4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(approvedReturnId))
                .andExpect(jsonPath("$.content[0].trackingNumber").value(tracking));

        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .param("dateFrom", OffsetDateTime.now().minusDays(1).toInstant().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(approvedReturnId));

        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .param("size", "1")
                        .param("sort", "requestedAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(requestedReturnId));
        mockMvc.perform(get("/api/v1/admin/returns")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "requestedAt,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(approvedReturnId));

        mockMvc.perform(patch("/api/v1/admin/returns/{id}/note", requestedReturnId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/admin/returns/{id}/note", requestedReturnId)
                        .header("Authorization", "Bearer " + normal.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "Not allowed"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/returns/{id}/note", requestedReturnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "  Customer contacted; awaiting photos.  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").value("Customer contacted; awaiting photos."));
        mockMvc.perform(patch("/api/v1/admin/returns/{id}/note", requestedReturnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", ""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").doesNotExist());
    }
    private String deliveredOrder(AuthTokens admin, AuthTokens owner, UUID variantId, int quantity) throws Exception {
        String orderId = createOrder(owner, variantId, quantity);
        createPayment(owner, orderId);
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PROCESSING"))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carrier", "DHL", "trackingNumber", "DHL-RETURN-" + orderId.substring(0, 8)))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELIVERED"))))
                .andExpect(status().isOk());
        return orderId;
    }

    private String createReturn(AuthTokens owner, String orderId, String orderItemId, int quantity) throws Exception {
        var created = mockMvc.perform(post("/api/v1/orders/{orderId}/returns", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnPayload(orderItemId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }


    private void approveReturn(AuthTokens admin, String returnId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/returns/{id}/approve", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNote", "Approved"))))
                .andExpect(status().isOk());
    }

    private void generateLabel(AuthTokens admin, String returnId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/returns/{id}/shipping-label", returnId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment.trackingNumber").exists());
    }

    private int returnShipmentCount(String returnId) {
        return jdbcTemplate.queryForObject("select count(*) from return_shipments where return_id = ?", Integer.class, UUID.fromString(returnId));
    }

    private String returnShippedAt(String returnId) {
        return jdbcTemplate.queryForObject("select cast(shipped_at as varchar(80)) from return_shipments where return_id = ?", String.class, UUID.fromString(returnId));
    }    private String returnTracking(String returnId) {
        return jdbcTemplate.queryForObject("select tracking_number from return_shipments where return_id = ?", String.class, UUID.fromString(returnId));
    }

    private String returnPayload(String orderItemId, int quantity) throws Exception {
        return json(Map.of(
                "customerNote", "Please review this return",
                "items", List.of(Map.of("orderItemId", orderItemId, "quantity", quantity, "reason", "Changed mind"))));
    }

    private void createPayment(AuthTokens owner, String orderId) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isCreated());
    }

    private String createOrder(AuthTokens tokens, UUID variantId, int quantity) throws Exception {
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variantId, quantity));
        var created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }

    private AuthTokens registerAdmin(String email) throws Exception {
        AuthTokens created = registerUser(email);
        promoteToAdmin(created.userId());
        return login(email);
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Password123!", "firstName", "Lux", "lastName", "Return"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private AuthTokens login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private void promoteToAdmin(String userId) {
        UUID roleId = adminRoleId();
        jdbcTemplate.update("insert into user_roles (user_id, role_id) values (?, ?)", UUID.fromString(userId), roleId);
    }

    private UUID adminRoleId() {
        try {
            return jdbcTemplate.queryForObject("select id from roles where name = ?", UUID.class, "ROLE_ADMIN");
        } catch (EmptyResultDataAccessException exception) {
            UUID id = UUID.randomUUID();
            jdbcTemplate.update("insert into roles (id, name) values (?, ?)", id, "ROLE_ADMIN");
            return id;
        }
    }

    private void ensureAdminRole() { adminRoleId(); }

    private ProductVariant variant(String sku) {
        return variantRepository.findAll().stream().filter(variant -> sku.equals(variant.getSku())).findFirst().orElseThrow();
    }

    private String orderItemId(String orderId) {
        return jdbcTemplate.queryForObject("select id from order_items where order_id = ?", UUID.class, UUID.fromString(orderId)).toString();
    }

    private BigDecimal unitPrice(String orderId) {
        return jdbcTemplate.queryForObject("select unit_price from order_items where order_id = ?", BigDecimal.class, UUID.fromString(orderId));
    }

    private int stock(UUID variantId) {
        return jdbcTemplate.queryForObject("select quantity_available from inventory_items where variant_id = ?", Integer.class, variantId);
    }

    private void resetStock(String sku, int quantity) {
        jdbcTemplate.update("update inventory_items set quantity_available = ? where variant_id in (select id from product_variants where sku = ?)", quantity, sku);
    }

    private int successfulReturnRefundCount(String returnId) {
        return jdbcTemplate.queryForObject("select count(*) from refunds where return_id = ? and status = 'SUCCEEDED'", Integer.class, UUID.fromString(returnId));
    }

    private int returnHistoryCount(String returnId) {
        return jdbcTemplate.queryForObject("select count(*) from return_status_history where return_id = ?", Integer.class, UUID.fromString(returnId));
    }

    private String orderStatus(String orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, UUID.fromString(orderId));
    }

    private String userKey(AuthTokens tokens) { return "user:" + tokens.userId(); }

    private String validAddress() throws Exception {
        return json(Map.of("recipientName", "Lux Return", "phone", "+84901234567", "addressLine1", "1 Dong Khoi", "addressLine2", "Suite 8", "city", "Ho Chi Minh City", "province", "Ho Chi Minh", "country", "Vietnam", "postalCode", "700000"));
    }

    private String json(Object value) throws Exception { return objectMapper.writeValueAsString(value); }

    private record AuthTokens(String accessToken, String userId) {
    }
}




