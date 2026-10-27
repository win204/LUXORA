package com.luxora.commerce.payment.provider;

import com.luxora.commerce.payment.config.VnPayProperties;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VnPayPaymentProviderUnitTests {

    private VnPayProperties properties;
    private VnPayPaymentProvider provider;

    @BeforeEach
    void setUp() {
        properties = new VnPayProperties(
                "TESTTMN",
                "TESTSECRETKEY1234567890ABCDEF",
                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                "http://localhost:3000/orders");
        provider = new VnPayPaymentProvider(properties);
    }

    @Test
    void buildPaymentUrlGeneratesValidUrlWithSignature() {
        String paymentUrl = provider.buildPaymentUrl(
                "order-123",
                new BigDecimal("500000"),
                "VND",
                "NCB",
                "127.0.0.1",
                null,
                "vnp_order-123_1000");

        assertThat(paymentUrl).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(paymentUrl).contains("vnp_TmnCode=TESTTMN");
        assertThat(paymentUrl).contains("vnp_Amount=50000000");
        assertThat(paymentUrl).contains("vnp_BankCode=NCB");
        assertThat(paymentUrl).contains("vnp_TxnRef=vnp_order-123_1000");
        assertThat(paymentUrl).contains("vnp_SecureHash=");
    }

    @Test
    void verifySignatureValidatesCorrectSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_OrderInfo", "Thanh toan don hang test");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "TESTTMN");
        params.put("vnp_TxnRef", "vnp_test_123");

        // Calculate expected hash
        String hashData = "vnp_Amount=10000000&vnp_BankCode=NCB&vnp_OrderInfo=Thanh+toan+don+hang+test&vnp_ResponseCode=00&vnp_TmnCode=TESTTMN&vnp_TxnRef=vnp_test_123";
        String expectedHash = VnPayPaymentProvider.hmacSHA512("TESTSECRETKEY1234567890ABCDEF", hashData);
        params.put("vnp_SecureHash", expectedHash);

        assertThat(provider.verifySignature(params)).isTrue();
    }

    @Test
    void verifySignatureRejectsTamperedParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "TESTTMN");
        params.put("vnp_TxnRef", "vnp_test_123");
        params.put("vnp_SecureHash", "invalid_signature");

        assertThat(provider.verifySignature(params)).isFalse();
    }
}
