package com.luxora.commerce.payment.provider;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class MockPaymentProvider implements PaymentProvider {

    private static final String PROVIDER = "MOCK";
    private static final String FAILED = "FAILED";

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public PaymentProviderResult charge(PaymentProviderRequest request) {
        boolean succeeded = !FAILED.equals(request.mockOutcome());
        String reference = PROVIDER.toLowerCase() + "_" + request.orderId() + "_" + UUID.randomUUID();
        return new PaymentProviderResult(reference, succeeded);
    }
}