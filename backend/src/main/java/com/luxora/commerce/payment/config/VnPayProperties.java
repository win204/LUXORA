package com.luxora.commerce.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "luxora.payment.vnpay")
public record VnPayProperties(
        String tmnCode,
        String hashSecret,
        String payUrl,
        String returnUrl) {

    public VnPayProperties {
        if (tmnCode == null || tmnCode.isBlank()) {
            tmnCode = "DEMOVNPAY";
        }
        if (hashSecret == null || hashSecret.isBlank()) {
            hashSecret = "DEMOHASHSECRETKEY1234567890";
        }
        if (payUrl == null || payUrl.isBlank()) {
            payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        }
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "http://localhost:3000/orders";
        }
    }
}
