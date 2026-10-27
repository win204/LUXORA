package com.luxora.commerce.shipping.provider;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component("mockShippingProvider")
public class MockShippingProvider implements ShippingProvider {

    @Override
    public String getCarrierCode() {
        return "MOCK";
    }

    @Override
    public ShippingOrderResult createShippingOrder(ShippingOrderRequest request) {
        String trackingNumber = "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return new ShippingOrderResult(
                getCarrierCode(),
                trackingNumber,
                buildTrackingUrl(trackingNumber),
                BigDecimal.ZERO,
                Instant.now().plus(3, ChronoUnit.DAYS));
    }

    @Override
    public String buildTrackingUrl(String trackingNumber) {
        return null;
    }
}
