package com.luxora.commerce.order.dto;

import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.order.shipment.dto.ShipmentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
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
}
