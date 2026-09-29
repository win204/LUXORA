package com.luxora.commerce.admin.api;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.BrandRepository;
import com.luxora.commerce.catalog.repository.CategoryRepository;
import com.luxora.commerce.catalog.repository.ProductRepository;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
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

import static org.mockito.Mockito.reset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
        resetStock("AUR-X1-GRF-128", 20);
        resetStock("AUR-X1-SLV-256", 8);
        ensureAdminRole();
    }

    @Test
    void unauthenticatedAdminApiRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void normalUserAdminApiForbidden() throws Exception {
        AuthTokens user = registerUser("admin-normal-forbidden@example.com");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanReadDashboard() throws Exception {
        AuthTokens admin = registerAdmin("admin-dashboard@example.com");

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value((int) productRepository.count()))
                .andExpect(jsonPath("$.totalVariants").exists())
                .andExpect(jsonPath("$.totalOrders").value(0));
    }

    @Test
    void adminProductPaginationWorks() throws Exception {
        AuthTokens admin = registerAdmin("admin-products-page@example.com");

        mockMvc.perform(get("/api/v1/admin/products?page=0&size=2")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].variantCount").exists());
    }

    @Test
    void adminOrderPaginationWorks() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-orders-page@example.com");
        AuthTokens first = registerUser("admin-order-first@example.com");
        AuthTokens second = registerUser("admin-order-second@example.com");
        createOrder(first, variant.getId());
        createOrder(second, variant.getId());

        mockMvc.perform(get("/api/v1/admin/orders?page=0&size=1")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userEmail").exists());
    }

    @Test
    void adminCanViewAnyOrder() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-view-order@example.com");
        AuthTokens owner = registerUser("admin-order-owner@example.com");
        String orderId = createOrder(owner, variant.getId());

        mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userEmail").value("admin-order-owner@example.com"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void normalUserStillCannotAccessAnotherUsersNormalOrderEndpoint() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("admin-normal-order-owner@example.com");
        AuthTokens other = registerUser("admin-normal-order-other@example.com");
        String orderId = createOrder(owner, variant.getId());

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }


    @Test
    void adminCanCreateProduct() throws Exception {
        AuthTokens admin = registerAdmin("admin-create-product@example.com");

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload("codex-admin-product", "Codex Admin Product")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("codex-admin-product"))
                .andExpect(jsonPath("$.brandId").exists())
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.variants.length()").value(0));
    }

    @Test
    void duplicateProductSlugReturnsConflict() throws Exception {
        AuthTokens admin = registerAdmin("admin-duplicate-slug@example.com");
        createAdminProduct(admin, "codex-duplicate-slug", "Codex Duplicate A");

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload("codex-duplicate-slug", "Codex Duplicate B")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_SLUG_CONFLICT"));
    }

    @Test
    void duplicateVariantSkuReturnsConflict() throws Exception {
        AuthTokens admin = registerAdmin("admin-duplicate-sku@example.com");
        String productId = createAdminProduct(admin, "codex-sku-product", "Codex SKU Product");
        createVariant(admin, productId, "COD-SKU-001", "1000000.00", 4);

        mockMvc.perform(post("/api/v1/admin/products/{id}/variants", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantPayload("COD-SKU-001", "1200000.00", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VARIANT_SKU_CONFLICT"));
    }

    @Test
    void invalidVariantPriceRejected() throws Exception {
        AuthTokens admin = registerAdmin("admin-invalid-price@example.com");
        String productId = createAdminProduct(admin, "codex-invalid-price", "Codex Invalid Price");

        mockMvc.perform(post("/api/v1/admin/products/{id}/variants", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantPayload("COD-PRICE-001", "-1.00", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidInventoryQuantityRejected() throws Exception {
        AuthTokens admin = registerAdmin("admin-invalid-stock@example.com");
        String productId = createAdminProduct(admin, "codex-invalid-stock", "Codex Invalid Stock");
        String variantId = createVariant(admin, productId, "COD-STOCK-001", "1000000.00", 2);

        mockMvc.perform(patch("/api/v1/admin/inventory/{variantId}", variantId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantityAvailable", -1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void normalUserCannotCreateAdminProduct() throws Exception {
        AuthTokens user = registerUser("admin-write-normal@example.com");

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload("codex-forbidden-product", "Codex Forbidden Product")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedCannotCreateAdminProduct() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload("codex-unauth-product", "Codex Unauth Product")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminCanUpdateProduct() throws Exception {
        AuthTokens admin = registerAdmin("admin-update-product@example.com");
        String productId = createAdminProduct(admin, "codex-update-product", "Codex Update Product");

        mockMvc.perform(patch("/api/v1/admin/products/{id}", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload("codex-update-product-renamed", "Codex Update Product Renamed")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("codex-update-product-renamed"))
                .andExpect(jsonPath("$.name").value("Codex Update Product Renamed"));
    }

    @Test
    void adminCanUpdateVariant() throws Exception {
        AuthTokens admin = registerAdmin("admin-update-variant@example.com");
        String productId = createAdminProduct(admin, "codex-update-variant", "Codex Update Variant");
        String variantId = createVariant(admin, productId, "COD-VAR-001", "1000000.00", 3);

        mockMvc.perform(patch("/api/v1/admin/variants/{id}", variantId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantUpdatePayload("COD-VAR-002", "1250000.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].sku").value("COD-VAR-002"))
                .andExpect(jsonPath("$.variants[0].price").value(1250000.00));
    }

    @Test
    void adminCanUpdateInventory() throws Exception {
        AuthTokens admin = registerAdmin("admin-update-inventory@example.com");
        String productId = createAdminProduct(admin, "codex-update-inventory", "Codex Update Inventory");
        String variantId = createVariant(admin, productId, "COD-INV-001", "1000000.00", 3);

        mockMvc.perform(patch("/api/v1/admin/inventory/{variantId}", variantId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("quantityAvailable", 9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(9))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void adminCanAddAndRemoveImage() throws Exception {
        AuthTokens admin = registerAdmin("admin-image@example.com");
        String productId = createAdminProduct(admin, "codex-image", "Codex Image");
        var added = mockMvc.perform(post("/api/v1/admin/products/{id}/images", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("url", "https://example.com/codex-image.jpg", "altText", "Codex image", "displayOrder", 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andReturn();
        String imageId = objectMapper.readTree(added.getResponse().getContentAsString()).get("images").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/admin/products/{id}/images/{imageId}", productId, imageId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(0));
    }

    @Test
    void adminCanAddAndRemoveSpecification() throws Exception {
        AuthTokens admin = registerAdmin("admin-spec@example.com");
        String productId = createAdminProduct(admin, "codex-spec", "Codex Spec");
        var added = mockMvc.perform(post("/api/v1/admin/products/{id}/specifications", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Material", "value", "Aluminum", "displayOrder", 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specifications.length()").value(1))
                .andReturn();
        String specId = objectMapper.readTree(added.getResponse().getContentAsString()).get("specifications").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/admin/products/{id}/specifications/{specId}", productId, specId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specifications.length()").value(0));
    }

    @Test
    void publicCatalogReflectsAdminChanges() throws Exception {
        AuthTokens admin = registerAdmin("admin-public-catalog@example.com");
        String productId = createAdminProduct(admin, "codex-public-product", "Codex Public Product");
        createVariant(admin, productId, "COD-PUB-001", "1500000.00", 5);
        mockMvc.perform(post("/api/v1/admin/products/{id}/images", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("url", "https://example.com/codex-public.jpg", "altText", "Codex public", "displayOrder", 0))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/admin/products/{id}/specifications", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Finish", "value", "Graphite", "displayOrder", 0))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products/codex-public-product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Codex Public Product"))
                .andExpect(jsonPath("$.variants[0].sku").value("COD-PUB-001"))
                .andExpect(jsonPath("$.variants[0].inStock").value(true))
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.specifications.length()").value(1));
    }


    @Test
    void adminCanTransitionPaidToProcessingWithHistory() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-transition-processing@example.com");
        AuthTokens owner = registerUser("admin-transition-processing-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PAID");
        String itemNameBefore = orderItemName(orderId);

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PROCESSING"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.statusHistory.length()").value(1))
                .andExpect(jsonPath("$.statusHistory[0].fromStatus").value("PAID"))
                .andExpect(jsonPath("$.statusHistory[0].toStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.statusHistory[0].changedByUserId").value(admin.userId()));

        assertThat(orderStatus(orderId)).isEqualTo("PROCESSING");
        assertThat(orderItemName(orderId)).isEqualTo(itemNameBefore);
    }

    @Test
    void adminCreatesShipmentFromProcessingAndMarksShipped() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-shipment-create@example.com");
        AuthTokens owner = registerUser("admin-shipment-create-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PROCESSING");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shipmentPayload("DHL", "DHL-1001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shipment.carrier").value("DHL"))
                .andExpect(jsonPath("$.shipment.trackingNumber").value("DHL-1001"))
                .andExpect(jsonPath("$.shipment.shippedAt").exists())
                .andExpect(jsonPath("$.statusHistory[0].fromStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.statusHistory[0].toStatus").value("SHIPPED"));
        assertThat(shipmentCount(orderId)).isEqualTo(1);
    }

    @Test
    void shippedOrderCanBeDeliveredAndSetsDeliveredAt() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-transition-delivered@example.com");
        AuthTokens owner = registerUser("admin-transition-delivered-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PROCESSING");
        createShipment(admin, orderId, "UPS", "UPS-DELIVERED");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELIVERED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"))
                .andExpect(jsonPath("$.shipment.deliveredAt").exists())
                .andExpect(jsonPath("$.statusHistory[1].fromStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.statusHistory[1].toStatus").value("DELIVERED"));
        assertThat(deliveredAt(orderId)).isNotNull();
    }

    @Test
    void invalidAdminTransitionRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-transition-invalid@example.com");
        AuthTokens owner = registerUser("admin-transition-invalid-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PENDING");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELIVERED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_STATUS_TRANSITION_INVALID"));
        assertThat(historyCount(orderId)).isZero();
    }

    @Test
    void normalUserCannotTransitionOrderStatus() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("admin-transition-normal-owner@example.com");
        AuthTokens user = registerUser("admin-transition-normal-user@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PAID");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PROCESSING"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedCannotTransitionOrderStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PROCESSING"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void paymentSuccessStillMarksPendingOrderPaid() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("admin-transition-payment-owner@example.com");
        String orderId = createOrder(owner, variant.getId());

        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
    }

    @Test
    void customerOrderApiStillReturnsOwnerOrderAfterAdminTransition() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-transition-customer-admin@example.com");
        AuthTokens owner = registerUser("admin-transition-customer-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PAID");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PROCESSING"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void paidRefundSuccessCancelsOrderAndRestoresStock() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-refund-paid@example.com");
        AuthTokens owner = registerUser("admin-refund-paid-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        int stockAfterOrder = stock(variant.getId());
        createPayment(owner, orderId, "SUCCEEDED");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED", "reason", "Customer request"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.latestRefund.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.latestRefund.provider").value("MOCK"))
                .andExpect(jsonPath("$.latestRefund.reason").value("Customer request"))
                .andExpect(jsonPath("$.statusHistory[0].fromStatus").value("PAID"))
                .andExpect(jsonPath("$.statusHistory[0].toStatus").value("CANCELLED"));

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder + 1);
        assertThat(successfulRefundCount(orderId)).isEqualTo(1);
        assertThat(cancelHistoryCount(orderId)).isEqualTo(1);
    }

    @Test
    void processingRefundSuccessCancelsOrderAndRestoresStock() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-refund-processing@example.com");
        AuthTokens owner = registerUser("admin-refund-processing-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        int stockAfterOrder = stock(variant.getId());
        createPayment(owner, orderId, "SUCCEEDED");
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PROCESSING"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.latestRefund.status").value("SUCCEEDED"));

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder + 1);
        assertThat(cancelHistoryCount(orderId)).isEqualTo(1);
    }

    @Test
    void refundFailureLeavesOrderAndInventoryUnchanged() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-refund-failure@example.com");
        AuthTokens owner = registerUser("admin-refund-failure-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        int stockAfterOrder = stock(variant.getId());
        createPayment(owner, orderId, "SUCCEEDED");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "FAILED", "reason", "Mock decline"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.latestRefund.status").value("FAILED"));

        assertThat(orderStatus(orderId)).isEqualTo("PAID");
        assertThat(stock(variant.getId())).isEqualTo(stockAfterOrder);
        assertThat(successfulRefundCount(orderId)).isZero();
        assertThat(cancelHistoryCount(orderId)).isZero();
    }

    @Test
    void repeatedRefundAfterSuccessIsIdempotent() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-refund-repeat@example.com");
        AuthTokens owner = registerUser("admin-refund-repeat-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        int stockAfterOrder = stock(variant.getId());
        createPayment(owner, orderId, "SUCCEEDED");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        int stockAfterRefund = stock(variant.getId());

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(stockAfterRefund).isEqualTo(stockAfterOrder + 1);
        assertThat(stock(variant.getId())).isEqualTo(stockAfterRefund);
        assertThat(successfulRefundCount(orderId)).isEqualTo(1);
        assertThat(refundCount(orderId)).isEqualTo(1);
        assertThat(cancelHistoryCount(orderId)).isEqualTo(1);
    }

    @Test
    void nonRefundableStatusesRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-refund-reject@example.com");
        AuthTokens owner = registerUser("admin-refund-reject-owner@example.com");

        String pendingOrderId = createOrder(owner, variant.getId());
        assertRefundRejected(admin, pendingOrderId);

        String shippedOrderId = createOrder(owner, variant.getId());
        createPayment(owner, shippedOrderId, "SUCCEEDED");
        markOrderStatus(shippedOrderId, "SHIPPED");
        assertRefundRejected(admin, shippedOrderId);

        String deliveredOrderId = createOrder(owner, variant.getId());
        createPayment(owner, deliveredOrderId, "SUCCEEDED");
        markOrderStatus(deliveredOrderId, "DELIVERED");
        assertRefundRejected(admin, deliveredOrderId);
    }

    @Test
    void unauthenticatedCannotRefundAndCancel() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void normalUserCannotRefundAndCancel() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("admin-refund-normal-owner@example.com");
        AuthTokens normal = registerUser("admin-refund-normal@example.com");
        String orderId = createOrder(owner, variant.getId());
        createPayment(owner, orderId, "SUCCEEDED");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + normal.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void refundUsesPersistedPaymentAmountAndCurrency() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-refund-authoritative@example.com");
        AuthTokens owner = registerUser("admin-refund-authoritative-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        createPayment(owner, orderId, "SUCCEEDED");
        BigDecimal persistedAmount = paymentAmount(orderId);

        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "mockOutcome", "SUCCEEDED",
                                "amount", "1.00",
                                "currency", "VND"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestRefund.currency").value("USD"));

        assertThat(refundAmount(orderId)).isEqualByComparingTo(persistedAmount);
        assertThat(refundCurrency(orderId)).isEqualTo("USD");
    }
    private void createPayment(AuthTokens owner, String orderId, String outcome) throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/payments", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", outcome))))
                .andExpect(status().isCreated());
    }

    private void assertRefundRejected(AuthTokens admin, String orderId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/refund-and-cancel", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mockOutcome", "SUCCEEDED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_REFUND_CANCEL_INVALID"));
    }

    private int refundCount(String orderId) {
        return jdbcTemplate.queryForObject("select count(*) from refunds where order_id = ?", Integer.class, UUID.fromString(orderId));
    }

    private int successfulRefundCount(String orderId) {
        return jdbcTemplate.queryForObject("select count(*) from refunds where order_id = ? and status = 'SUCCEEDED'", Integer.class, UUID.fromString(orderId));
    }

    private int cancelHistoryCount(String orderId) {
        return jdbcTemplate.queryForObject("select count(*) from order_status_history where order_id = ? and to_status = 'CANCELLED'", Integer.class, UUID.fromString(orderId));
    }

    private BigDecimal paymentAmount(String orderId) {
        return jdbcTemplate.queryForObject("select top 1 amount from payments where order_id = ? and status = 'SUCCEEDED' order by created_at desc", BigDecimal.class, UUID.fromString(orderId));
    }

    private BigDecimal refundAmount(String orderId) {
        return jdbcTemplate.queryForObject("select top 1 amount from refunds where order_id = ? and status = 'SUCCEEDED' order by created_at desc", BigDecimal.class, UUID.fromString(orderId));
    }

    private String refundCurrency(String orderId) {
        return jdbcTemplate.queryForObject("select top 1 currency from refunds where order_id = ? and status = 'SUCCEEDED' order by created_at desc", String.class, UUID.fromString(orderId));
    }
    @Test
    void shipmentCreationRejectsInvalidStatus() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-shipment-invalid-status@example.com");
        AuthTokens owner = registerUser("admin-shipment-invalid-status-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PAID");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shipmentPayload("DHL", "DHL-INVALID")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_SHIPMENT_INVALID_STATUS"));
    }

    @Test
    void shipmentCreationValidatesCarrierAndTracking() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-shipment-validation@example.com");
        AuthTokens owner = registerUser("admin-shipment-validation-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PROCESSING");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("carrier", "", "trackingNumber", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void duplicateShipmentRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-shipment-duplicate@example.com");
        AuthTokens owner = registerUser("admin-shipment-duplicate-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PROCESSING");
        createShipment(admin, orderId, "DHL", "DHL-FIRST");
        markOrderStatus(orderId, "PROCESSING");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shipmentPayload("DHL", "DHL-DUPLICATE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_SHIPMENT_EXISTS"));
    }

    @Test
    void adminCanUpdateTrackingWhileShipped() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-shipment-update@example.com");
        AuthTokens owner = registerUser("admin-shipment-update-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PROCESSING");
        createShipment(admin, orderId, "DHL", "DHL-OLD");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shipmentPayload("FedEx", "FDX-NEW")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shipment.carrier").value("FedEx"))
                .andExpect(jsonPath("$.shipment.trackingNumber").value("FDX-NEW"));
    }

    @Test
    void normalUserCannotUseAdminShipmentApi() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens owner = registerUser("admin-shipment-normal-owner@example.com");
        AuthTokens normal = registerUser("admin-shipment-normal@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PROCESSING");

        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + normal.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shipmentPayload("DHL", "DHL-FORBIDDEN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void customerSeesOwnShipmentButNotAnotherOrder() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-shipment-customer-admin@example.com");
        AuthTokens owner = registerUser("admin-shipment-customer-owner@example.com");
        AuthTokens other = registerUser("admin-shipment-customer-other@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "PROCESSING");
        createShipment(admin, orderId, "DHL", "DHL-CUSTOMER");

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipment.carrier").value("DHL"))
                .andExpect(jsonPath("$.shipment.trackingNumber").value("DHL-CUSTOMER"));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void cannotDeliverWithoutShipment() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-shipment-deliver-missing@example.com");
        AuthTokens owner = registerUser("admin-shipment-deliver-missing-owner@example.com");
        String orderId = createOrder(owner, variant.getId());
        markOrderStatus(orderId, "SHIPPED");

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELIVERED"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_SHIPMENT_REQUIRED"));
    }
    @Test
    void adminOrderQueueSupportsFiltersSortingPaginationAndOperationalNotes() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens admin = registerAdmin("admin-order-queue@example.com");
        AuthTokens firstOwner = registerUser("admin-order-queue-first@example.com");
        AuthTokens secondOwner = registerUser("admin-order-queue-second@example.com");
        AuthTokens normalUser = registerUser("admin-order-queue-normal@example.com");
        String pendingOrderId = createOrder(firstOwner, variant.getId());
        Thread.sleep(5);
        String shippedOrderId = createOrder(secondOwner, variant.getId());
        markOrderStatus(shippedOrderId, "PROCESSING");
        createShipment(admin, shippedOrderId, "DHL", "DHL-ORDER-QUEUE-1");

        mockMvc.perform(get("/api/v1/admin/orders?status=PENDING")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(pendingOrderId))
                .andExpect(jsonPath("$.content[0].totalItems").value(1));

        mockMvc.perform(get("/api/v1/admin/orders?customerEmail=queue-second")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(shippedOrderId));

        mockMvc.perform(get("/api/v1/admin/orders?orderId={id}", pendingOrderId)
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(pendingOrderId));

        mockMvc.perform(get("/api/v1/admin/orders?trackingNumber=DHL-ORDER-QUEUE-1")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(shippedOrderId))
                .andExpect(jsonPath("$.content[0].trackingNumber").value("DHL-ORDER-QUEUE-1"));

        mockMvc.perform(get("/api/v1/admin/orders?dateFrom=2000-01-01T00:00:00Z&dateTo=2100-01-01T00:00:00Z&page=0&size=1&sort=createdAt,asc")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(pendingOrderId));

        mockMvc.perform(get("/api/v1/admin/orders?sort=createdAt,desc")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(shippedOrderId));

        mockMvc.perform(get("/api/v1/admin/orders?sort=user.email,asc")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_SORT_INVALID"));

        mockMvc.perform(get("/api/v1/admin/orders?dateFrom=2100-01-01T00:00:00Z&dateTo=2000-01-01T00:00:00Z")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_DATE_RANGE_INVALID"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/note", pendingOrderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "  Confirm address before fulfillment.  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").value("Confirm address before fulfillment."))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/note", pendingOrderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", " "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminNote").doesNotExist());

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/note", pendingOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "Denied"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(patch("/api/v1/admin/orders/{id}/note", pendingOrderId)
                        .header("Authorization", "Bearer " + normalUser.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "Denied"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
    private void createShipment(AuthTokens admin, String orderId, String carrier, String trackingNumber) throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/shipment", orderId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shipmentPayload(carrier, trackingNumber)))
                .andExpect(status().isOk());
    }

    private String shipmentPayload(String carrier, String trackingNumber) throws Exception {
        return json(Map.of("carrier", carrier, "trackingNumber", trackingNumber));
    }

    private int shipmentCount(String orderId) {
        return jdbcTemplate.queryForObject("select count(*) from shipments where order_id = ?", Integer.class, UUID.fromString(orderId));
    }

    private Object deliveredAt(String orderId) {
        return jdbcTemplate.queryForObject("select delivered_at from shipments where order_id = ?", Object.class, UUID.fromString(orderId));
    }
    private AuthTokens registerAdmin(String email) throws Exception {
        AuthTokens created = registerUser(email);
        promoteToAdmin(created.userId());
        return login(email);
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Lux",
                                "lastName", "Admin"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private AuthTokens login(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private String createOrder(AuthTokens tokens, UUID variantId) throws Exception {
        when(cartStore.getQuantities(userKey(tokens))).thenReturn(Map.of(variantId, 1));
        var created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }



    private void markOrderStatus(String orderId, String status) {
        jdbcTemplate.update("update orders set status = ? where id = ?", status, UUID.fromString(orderId));
    }

    private String orderStatus(String orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, UUID.fromString(orderId));
    }

    private int historyCount(String orderId) {
        return jdbcTemplate.queryForObject("select count(*) from order_status_history where order_id = ?", Integer.class, UUID.fromString(orderId));
    }

    private String orderItemName(String orderId) {
        return jdbcTemplate.queryForObject("select product_name from order_items where order_id = ?", String.class, UUID.fromString(orderId));
    }

    private String createAdminProduct(AuthTokens admin, String slug, String name) throws Exception {
        var created = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload(slug, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }

    private String createVariant(AuthTokens admin, String productId, String sku, String price, int quantity) throws Exception {
        var created = mockMvc.perform(post("/api/v1/admin/products/{id}/variants", productId)
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantPayload(sku, price, quantity)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("variants").get(0).get("id").asText();
    }

    private String productPayload(String slug, String name) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("slug", slug);
        payload.put("subtitle", "Admin managed product");
        payload.put("description", "A product created through the LUXORA admin foundation.");
        payload.put("brandId", brandId());
        payload.put("categoryId", categoryId());
        payload.put("active", true);
        return json(payload);
    }

    private String variantPayload(String sku, String price, int quantity) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sku", sku);
        payload.put("color", "Graphite");
        payload.put("storage", "128 GB");
        payload.put("price", new BigDecimal(price));
        payload.put("active", true);
        payload.put("quantityAvailable", quantity);
        return json(payload);
    }

    private String variantUpdatePayload(String sku, String price) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sku", sku);
        payload.put("color", "Silver");
        payload.put("storage", "256 GB");
        payload.put("price", new BigDecimal(price));
        payload.put("active", true);
        return json(payload);
    }

    private UUID brandId() {
        return brandRepository.findByActiveTrueOrderByNameAsc().get(0).getId();
    }

    private UUID categoryId() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().get(0).getId();
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

    private void ensureAdminRole() {
        adminRoleId();
    }

    private ProductVariant variant(String sku) {
        return variantRepository.findAll().stream()
                .filter(variant -> sku.equals(variant.getSku()))
                .findFirst()
                .orElseThrow();
    }


    private int stock(UUID variantId) {
        return jdbcTemplate.queryForObject("select quantity_available from inventory_items where variant_id = ?", Integer.class, variantId);
    }
    private void resetStock(String sku, int quantity) {
        jdbcTemplate.update("update inventory_items set quantity_available = ? where variant_id in (select id from product_variants where sku = ?)", quantity, sku);
    }

    private String userKey(AuthTokens tokens) {
        return "user:" + tokens.userId();
    }

    private String validAddress() throws Exception {
        return json(Map.of(
                "recipientName", "Lux Admin",
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









