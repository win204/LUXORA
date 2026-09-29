package com.luxora.commerce.payment.provider;

import java.math.BigDecimal;

public interface PaymentProvider {

    String providerName();

    PaymentProviderResult charge(PaymentProviderRequest request);

    record PaymentProviderRequest(String orderId, BigDecimal amount, String currency, String mockOutcome) {
    }

    record PaymentProviderResult(String providerReference, boolean succeeded) {
    }
}