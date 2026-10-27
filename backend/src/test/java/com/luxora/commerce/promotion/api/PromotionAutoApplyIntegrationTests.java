package com.luxora.commerce.promotion.api;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PromotionAutoApplyIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductVariantRepository variantRepository;

    @MockitoBean
    private CartStore cartStore;

    @BeforeEach
    void resetCart() {
        reset(cartStore);
    }

    @Test
    void getAvailablePromotions_evaluatesEligibilityAndBestDeal() throws Exception {
        // At subtotal $1000:
        // LUXORA10 (10% off for >=$200, max $100) -> saves $100.00 (BEST DEAL)
        // WELCOME50 ($50 off for >=$300) -> saves $50.00
        mockMvc.perform(get("/api/v1/promotions/available?subtotal=1000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("LUXORA10"))
                .andExpect(jsonPath("$[0].eligible").value(true))
                .andExpect(jsonPath("$[0].potentialDiscount").value(100.0))
                .andExpect(jsonPath("$[0].isBestDeal").value(true))
                .andExpect(jsonPath("$[1].code").value("WELCOME50"))
                .andExpect(jsonPath("$[1].eligible").value(true))
                .andExpect(jsonPath("$[1].potentialDiscount").value(50.0))
                .andExpect(jsonPath("$[1].isBestDeal").value(false));

        // At subtotal $100:
        // Neither meets minimum order amount
        mockMvc.perform(get("/api/v1/promotions/available?subtotal=100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'LUXORA10')].eligible").value(false))
                .andExpect(jsonPath("$[?(@.code == 'LUXORA10')].amountNeeded").value(100.0))
                .andExpect(jsonPath("$[?(@.code == 'WELCOME50')].eligible").value(false))
                .andExpect(jsonPath("$[?(@.code == 'WELCOME50')].amountNeeded").value(200.0));
    }

    @Test
    void checkoutPreview_withAutoApplyBestVoucher_automaticallyAppliesHighestDiscountVoucher() throws Exception {
        ProductVariant variant = variant("AUR-X1-GRF-128"); // Price $899.00
        String cartId = "cart-auto-voucher-" + UUID.randomUUID();

        // 2 units = $1798.00 subtotal
        when(cartStore.getQuantities(startsWith("anon:" + cartId))).thenReturn(Map.of(variant.getId(), 2));

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("X-Cart-Id", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "guest@example.com",
                                "recipientName", "Voucher Collector",
                                "phone", "+1 555 987 6543",
                                "addressLine1", "789 Fifth Ave",
                                "city", "New York",
                                "province", "NY",
                                "country", "US",
                                "postalCode", "10022",
                                "autoApplyBestVoucher", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(1798.0))
                .andExpect(jsonPath("$.promotion.code").value("LUXORA10"))
                .andExpect(jsonPath("$.discount").value(100.0))
                .andExpect(jsonPath("$.grandTotal").value(1698.0))
                .andExpect(jsonPath("$.availablePromotions").isArray())
                .andExpect(jsonPath("$.availablePromotions.length()").isNotEmpty());
    }

    private ProductVariant variant(String sku) {
        return variantRepository.findAll().stream()
                .filter(v -> sku.equals(v.getSku()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Variant not found: " + sku));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
