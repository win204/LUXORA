package com.luxora.commerce.returning.provider;

import java.util.UUID;

public interface ReturnShippingProvider {

    ReturnShippingLabel createLabel(UUID returnId);

    record ReturnShippingLabel(String carrier, String trackingNumber, String mockLabelReference) {
    }
}