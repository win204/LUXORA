package com.luxora.commerce.returning.provider;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockReturnShippingProvider implements ReturnShippingProvider {

    private static final String PROVIDER_PREFIX = "LXR-RMA";

    @Override
    public ReturnShippingLabel createLabel(UUID returnId) {
        String shortId = returnId.toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return new ReturnShippingLabel("LUXORA Mock Returns", PROVIDER_PREFIX + "-" + shortId, "mock-return-label-" + shortId.toLowerCase(Locale.ROOT));
    }
}