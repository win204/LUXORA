package com.luxora.commerce.refund.provider;

import java.math.BigDecimal;

public interface RefundProvider {

    RefundProviderResult refund(RefundProviderRequest request);

    String providerName();

    record RefundProviderRequest(String orderId, String paymentId, BigDecimal amount, String currency, String outcome) {
    }

    record RefundProviderResult(boolean succeeded, String providerReference) {
    }
}
