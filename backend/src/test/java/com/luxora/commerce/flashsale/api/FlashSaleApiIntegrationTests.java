package com.luxora.commerce.flashsale.api;

import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductRepository;
import com.luxora.commerce.flashsale.dto.CreateFlashSaleItemRequest;
import com.luxora.commerce.flashsale.dto.CreateFlashSaleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class FlashSaleApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.luxora.commerce.catalog.repository.ProductVariantRepository productVariantRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.luxora.commerce.cart.service.CartStore cartStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("delete from flash_sale_items");
        jdbcTemplate.update("delete from flash_sales");
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
    void getActiveFlashSale_noneActive_returnsNoContent() throws Exception {
        mockMvc.perform(get("/api/v1/flash-sales/active"))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminEndpoints_unauthenticatedOrNonAdmin_returnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/flash-sales"))
                .andExpect(status().isUnauthorized());

        AuthTokens customer = registerUser("flashsale-customer@example.com");
        mockMvc.perform(get("/api/v1/admin/flash-sales")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFlashSale_byAdmin_succeedsAndReturnsActiveWithCountdown() throws Exception {
        String adminToken = loginAdmin();
        ProductVariant variant = productVariantRepository.findAll().stream()
                .filter(ProductVariant::isActive)
                .findFirst()
                .orElseThrow();
        Product product = variant.getProduct();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        CreateFlashSaleRequest req = new CreateFlashSaleRequest(
                "Midnight Luxury Flash Sale",
                "Exclusive midnight offers on high-end luxury goods",
                now.minusMinutes(10),
                now.plusHours(6),
                "https://images.unsplash.com/photo-1549465220-1a8b9238cd48",
                List.of(new CreateFlashSaleItemRequest(
                        product.getId(),
                        variant.getId(),
                        new BigDecimal("299.00"),
                        10
                ))
        );

        var createResult = mockMvc.perform(post("/api/v1/admin/flash-sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Midnight Luxury Flash Sale"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].flashSalePrice").value(299.00))
                .andExpect(jsonPath("$.items[0].stockQuantity").value(10))
                .andExpect(jsonPath("$.items[0].soldQuantity").value(0))
                .andExpect(jsonPath("$.items[0].percentClaimed").value(0))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        assertThat(created.get("timeRemainingSeconds").asLong()).isGreaterThan(0);

        // Public active endpoint should now return this flash sale
        mockMvc.perform(get("/api/v1/flash-sales/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Midnight Luxury Flash Sale"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].flashSalePrice").value(299.00));
    }

    @Test
    void checkoutPreview_andOrderCreation_withActiveFlashSale_appliesFlashSalePriceAndIncrementsSold() throws Exception {
        String adminToken = loginAdmin();
        ProductVariant variant = productVariantRepository.findAll().stream()
                .filter(ProductVariant::isActive)
                .findFirst()
                .orElseThrow();
        Product product = variant.getProduct();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        CreateFlashSaleRequest req = new CreateFlashSaleRequest(
                "VIP Flash Deal",
                "Limited quota flash sale",
                now.minusMinutes(5),
                now.plusHours(2),
                null,
                List.of(new CreateFlashSaleItemRequest(
                        product.getId(),
                        variant.getId(),
                        new BigDecimal("199.00"),
                        5
                ))
        );

        mockMvc.perform(post("/api/v1/admin/flash-sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isCreated());

        AuthTokens customer = registerUser("flashsale-buyer@example.com");

        // Mock customer cart in Redis store
        org.mockito.Mockito.when(cartStore.getQuantities(org.mockito.ArgumentMatchers.startsWith("user:"))).thenReturn(Map.of(variant.getId(), 2));

        // Checkout Preview should calculate unitPrice = 199.00 and subtotal = 398.00
        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "recipientName", "Flash Buyer",
                                "phone", "0901234567",
                                "addressLine1", "100 Luxury Blvd",
                                "city", "District 1",
                                "province", "Ho Chi Minh City",
                                "country", "VN",
                                "postalCode", "700000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(199.00))
                .andExpect(jsonPath("$.items[0].lineTotal").value(398.00))
                .andExpect(jsonPath("$.subtotal").value(398.00));

        // Create Order should increment soldQuantity in flash_sale_items by 2
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "recipientName", "Flash Buyer",
                                "phone", "0901234567",
                                "addressLine1", "100 Luxury Blvd",
                                "city", "District 1",
                                "province", "Ho Chi Minh City",
                                "country", "VN",
                                "postalCode", "700000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].unitPrice").value(199.00))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.grandTotal").value(398.00));

        // Verify active flash sale endpoint now reports soldQuantity = 2 and percentClaimed = 40% (2 / 5 * 100)
        mockMvc.perform(get("/api/v1/flash-sales/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].soldQuantity").value(2))
                .andExpect(jsonPath("$.items[0].percentClaimed").value(40));
    }

    @Test
    void cancelFlashSale_byAdmin_marksSaleCancelled() throws Exception {
        String adminToken = loginAdmin();
        ProductVariant variant = productVariantRepository.findAll().stream()
                .filter(ProductVariant::isActive)
                .findFirst()
                .orElseThrow();
        Product product = variant.getProduct();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        CreateFlashSaleRequest req = new CreateFlashSaleRequest(
                "Short-lived Sale",
                null,
                now.minusMinutes(1),
                now.plusHours(1),
                null,
                List.of(new CreateFlashSaleItemRequest(
                        product.getId(),
                        variant.getId(),
                        new BigDecimal("150.00"),
                        10
                ))
        );

        var createResult = mockMvc.perform(post("/api/v1/admin/flash-sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String saleId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Cancel sale
        mockMvc.perform(post("/api/v1/admin/flash-sales/" + saleId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Active sale should now return 204 No Content
        mockMvc.perform(get("/api/v1/flash-sales/active"))
                .andExpect(status().isNoContent());
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "Flash",
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
