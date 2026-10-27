package com.luxora.commerce.shipping.provider;

public interface ShippingProvider {

    String getCarrierCode();

    ShippingOrderResult createShippingOrder(ShippingOrderRequest request);

    String buildTrackingUrl(String trackingNumber);
}
