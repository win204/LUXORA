package com.luxora.commerce.payment.api;

import com.luxora.commerce.cart.service.CartStore;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.payment.config.VnPayProperties;
import com.luxora.commerce.payment.provider.VnPayPaymentProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class VnPayPaymentApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VnPayProperties vnPayProperties;

    @Autowired
    private VnPayPaymentProvider vnPayPaymentProvider;

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
        jdbcTemplate.update("update inventory_items set quantity_available = 50");
    }

    @Test
    void createVnPayPaymentGeneratesPaymentUrlAndKeepsPendingStatus() throws Exception {
        AuthTokens tokens = registerUser("vnpay-create@example.com");
        UUID orderId = createOrder(tokens);

        var result = mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "provider", "VNPAY",
                                "bankCode", "NCB"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("VNPAY"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentUrl").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String paymentUrl = json.get("paymentUrl").asText();
        assertThat(paymentUrl).startsWith(vnPayProperties.payUrl());
        assertThat(paymentUrl).contains("vnp_TmnCode=" + vnPayProperties.tmnCode());
        assertThat(paymentUrl).contains("vnp_SecureHash=");

        // Order should remain PENDING until IPN confirms
        String orderStatus = jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId);
        assertThat(orderStatus).isEqualTo("PENDING");
    }

    @Test
    void vnPayIpnWithValidSignatureMarksOrderPaid() throws Exception {
        AuthTokens tokens = registerUser("vnpay-ipn-success@example.com");
        UUID orderId = createOrder(tokens);

        // Initiate payment
        var result = mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("provider", "VNPAY"))))
                .andExpect(status().isCreated())
                .andReturn();

        String providerReference = objectMapper.readTree(result.getResponse().getContentAsString()).get("providerReference").asText();
        BigDecimal grandTotal = jdbcTemplate.queryForObject("select grand_total from orders where id = ?", BigDecimal.class, orderId);
        long amountInCents = grandTotal.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", String.valueOf(amountInCents));
        ipnParams.put("vnp_BankCode", "NCB");
        ipnParams.put("vnp_CardType", "ATM");
        ipnParams.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        ipnParams.put("vnp_PayDate", "20261020120000");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TmnCode", vnPayProperties.tmnCode());
        ipnParams.put("vnp_TransactionNo", "14000123");
        ipnParams.put("vnp_TransactionStatus", "00");
        ipnParams.put("vnp_TxnRef", providerReference);

        String secureHash = calculateHash(ipnParams, vnPayProperties.hashSecret());

        var req = get("/api/v1/payments/vnpay-ipn");
        for (Map.Entry<String, String> entry : ipnParams.entrySet()) {
            req = req.param(entry.getKey(), entry.getValue());
        }
        req = req.param("vnp_SecureHash", secureHash);

        mockMvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));

        String orderStatus = jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId);
        assertThat(orderStatus).isEqualTo("PAID");

        String paymentStatus = jdbcTemplate.queryForObject("select status from payments where provider_reference = ?", String.class, providerReference);
        assertThat(paymentStatus).isEqualTo("SUCCEEDED");
    }

    @Test
    void vnPayIpnWithInvalidChecksumIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "vnp_test_123")
                        .param("vnp_Amount", "10000000")
                        .param("vnp_SecureHash", "invalid_checksum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("Invalid Checksum"));
    }

    @Test
    void vnPayIpnWithUnknownOrderReturnsOrderNotFound() throws Exception {
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_Amount", "10000000");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TmnCode", vnPayProperties.tmnCode());
        ipnParams.put("vnp_TxnRef", "vnp_unknown_123");

        String secureHash = calculateHash(ipnParams, vnPayProperties.hashSecret());

        var req = get("/api/v1/payments/vnpay-ipn");
        for (Map.Entry<String, String> entry : ipnParams.entrySet()) {
            req = req.param(entry.getKey(), entry.getValue());
        }
        req = req.param("vnp_SecureHash", secureHash);

        mockMvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("01"))
                .andExpect(jsonPath("$.Message").value("Order Not Found"));
    }

    @Test
    void vnPayReturnEndpointValidatesSignature() throws Exception {
        Map<String, String> returnParams = new HashMap<>();
        returnParams.put("vnp_Amount", "10000000");
        returnParams.put("vnp_ResponseCode", "00");
        returnParams.put("vnp_TmnCode", vnPayProperties.tmnCode());
        returnParams.put("vnp_TxnRef", "vnp_return_123");

        String secureHash = calculateHash(returnParams, vnPayProperties.hashSecret());

        var req = get("/api/v1/payments/vnpay-return");
        for (Map.Entry<String, String> entry : returnParams.entrySet()) {
            req = req.param(entry.getKey(), entry.getValue());
        }
        req = req.param("vnp_SecureHash", secureHash);

        mockMvc.perform(req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validSignature").value(true))
                .andExpect(jsonPath("$.succeeded").value(true));
    }

    private String calculateHash(Map<String, String> params, String secretKey) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isBlank()) {
                String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII);
                hashData.append(fieldName).append('=').append(encodedValue);
                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                }
            }
        }
        return VnPayPaymentProvider.hmacSHA512(secretKey, hashData.toString());
    }

    private AuthTokens registerUser(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!",
                                "firstName", "VnPay",
                                "lastName", "Tester"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(response.get("accessToken").asText(), response.get("user").get("id").asText());
    }

    private UUID createOrder(AuthTokens tokens) throws Exception {
        ProductVariant variant = variantRepository.findAll().get(0);
        when(cartStore.getQuantities("user:" + tokens.userId())).thenReturn(Map.of(variant.getId(), 1));

        var created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recipientName", "VnPay User",
                                "phone", "+84901234567",
                                "addressLine1", "1 Nguyen Hue",
                                "city", "Ho Chi Minh City",
                                "province", "Ho Chi Minh",
                                "country", "Vietnam",
                                "postalCode", "700000"))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());
    }

    private record AuthTokens(String accessToken, String userId) {
    }
}
