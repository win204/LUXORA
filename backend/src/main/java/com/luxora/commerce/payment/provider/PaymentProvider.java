package com.luxora.commerce.payment.provider;

import java.math.BigDecimal;

public interface PaymentProvider {

    String providerName();

    PaymentProviderResult charge(PaymentProviderRequest request);

    record PaymentProviderRequest(
            String orderId,
            BigDecimal amount,
            String currency,
            String mockOutcome,
            String bankCode,
            String clientIp,
            String returnUrl) {

        public PaymentProviderRequest(String orderId, BigDecimal amount, String currency, String mockOutcome) {
            this(orderId, amount, currency, mockOutcome, null, null, null);
        }
    }

    record PaymentProviderResult(String providerReference, boolean succeeded, String paymentUrl) {

        public PaymentProviderResult(String providerReference, boolean succeeded) {
            this(providerReference, succeeded, null);
        }
    }
}