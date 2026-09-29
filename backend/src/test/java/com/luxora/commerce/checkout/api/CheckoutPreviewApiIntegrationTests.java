package com.luxora.commerce.checkout.api;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CheckoutPreviewApiIntegrationTests {

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
    void resetCartStore() {
        reset(cartStore);
    }

    @Test
    void authenticatedPreviewSucceedsWithAuthoritativeTotals() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("checkout-success@example.com");
        when(cartStore.getQuantities(startsWith("user:"))).thenReturn(Map.of(variant.getId(), 2));

        var result = mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].variantId").value(variant.getId().toString()))
                .andExpect(jsonPath("$.items[0].slug").value("aerophone-x1"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.shippingAddress.city").value("Ho Chi Minh City"))
                .andExpect(jsonPath("$.shippingFee").value(0))
                .andExpect(jsonPath("$.tax").value(0))
                .andExpect(jsonPath("$.discount").value(0))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal expectedLineTotal = variant.getPrice().multiply(BigDecimal.valueOf(2));
        assertThat(body.get("items").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo(variant.getPrice());
        assertThat(body.get("items").get(0).get("lineTotal").decimalValue()).isEqualByComparingTo(expectedLineTotal);
        assertThat(body.get("subtotal").decimalValue()).isEqualByComparingTo(expectedLineTotal);
        assertThat(body.get("grandTotal").decimalValue()).isEqualByComparingTo(expectedLineTotal);
    }

    @Test
    void unauthenticatedPreviewRejected() throws Exception {
        mockMvc.perform(post("/api/v1/checkout/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void emptyCartRejected() throws Exception {
        AuthTokens tokens = registerUser("checkout-empty@example.com");
        when(cartStore.getQuantities(startsWith("user:"))).thenReturn(Map.of());

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_CART"));
    }

    @Test
    void invalidAddressRejected() throws Exception {
        AuthTokens tokens = registerUser("checkout-invalid-address@example.com");

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("recipientName", "", "phone", "bad", "country", "Vietnam"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void clientSuppliedPricesAndTotalsAreIgnored() throws Exception {
        ProductVariant variant = variant("ATS-SO-MBK");
        AuthTokens tokens = registerUser("checkout-ignore-client-totals@example.com");
        when(cartStore.getQuantities(startsWith("user:"))).thenReturn(Map.of(variant.getId(), 3));

        var result = mockMvc.perform(post("/api/v1/checkout/preview")
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
                                "grandTotal", "1.00",
                                "items", java.util.List.of(Map.of("unitPrice", "1.00", "lineTotal", "1.00"))))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        BigDecimal expected = variant.getPrice().multiply(BigDecimal.valueOf(3));
        assertThat(body.get("subtotal").decimalValue()).isEqualByComparingTo(expected);
        assertThat(body.get("grandTotal").decimalValue()).isEqualByComparingTo(expected);
    }

    @Test
    void unavailableProductRejected() throws Exception {
        ProductVariant variant = variant("LMN-VS-SIL-256");
        AuthTokens tokens = registerUser("checkout-unavailable@example.com");
        when(cartStore.getQuantities(startsWith("user:"))).thenReturn(Map.of(variant.getId(), 1));

        jdbcTemplate.update("update product_variants set active = false where id = ?", variant.getId());
        try {
            mockMvc.perform(post("/api/v1/checkout/preview")
                            .header("Authorization", "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validAddress()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VARIANT_UNAVAILABLE"));
        } finally {
            jdbcTemplate.update("update product_variants set active = true where id = ?", variant.getId());
        }
    }

    @Test
    void insufficientStockRejected() throws Exception {
        ProductVariant variant = variant("AUR-X1-SLV-256");
        AuthTokens tokens = registerUser("checkout-stock@example.com");
        when(cartStore.getQuantities(startsWith("user:"))).thenReturn(Map.of(variant.getId(), 999));

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void previewDoesNotModifyCartStockOrCreateOrders() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128");
        AuthTokens tokens = registerUser("checkout-readonly@example.com");
        int stockBefore = stock(variant.getId());
        when(cartStore.getQuantities(startsWith("user:"))).thenReturn(Map.of(variant.getId(), 1));

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddress()))
                .andExpect(status().isOk());

        assertThat(stock(variant.getId())).isEqualTo(stockBefore);
        assertThat(orderTableCount()).isZero();
        verify(cartStore, never()).putQuantity(any(), any(), any(Integer.class), any());
        verify(cartStore, never()).removeItem(any(), any());
        verify(cartStore, never()).clear(any());
        verify(cartStore, never()).refreshTtl(any(), any());
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
        return new AuthTokens(response.get("accessToken").asText());
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

    private int orderTableCount() {
        return jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
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

    private record AuthTokens(String accessToken) {
    }
}
