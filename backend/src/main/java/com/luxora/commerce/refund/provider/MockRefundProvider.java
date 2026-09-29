package com.luxora.commerce.refund.provider;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockRefundProvider implements RefundProvider {

    @Override
    public RefundProviderResult refund(RefundProviderRequest request) {
        boolean succeeded = "SUCCEEDED".equals(request.outcome().toUpperCase(Locale.ROOT));
        return new RefundProviderResult(succeeded, "mock-refund-" + UUID.randomUUID());
    }

    @Override
    public String providerName() {
        return "MOCK";
    }
}
