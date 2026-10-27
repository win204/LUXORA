package com.luxora.commerce.order.dto;

import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.order.shipment.dto.ShipmentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerEmail,
        String guestAccessToken,
        String status,
        List<OrderItemResponse> items,
        ShippingAddressResponse shippingAddress,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal grandTotal,
        String currency,
        Instant createdAt,
        ShipmentResponse shipment,
        Integer loyaltyPointsRedeemed,
        BigDecimal loyaltyDiscount) {

    public OrderResponse(
            UUID id,
            String customerEmail,
            String guestAccessToken,
            String status,
            List<OrderItemResponse> items,
            ShippingAddressResponse shippingAddress,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal tax,
            BigDecimal discount,
            BigDecimal grandTotal,
            String currency,
            Instant createdAt,
            ShipmentResponse shipment) {
        this(id, customerEmail, guestAccessToken, status, items, shippingAddress,
                subtotal, shippingFee, tax, discount, grandTotal, currency, createdAt, shipment, 0, BigDecimal.ZERO);
    }
}
