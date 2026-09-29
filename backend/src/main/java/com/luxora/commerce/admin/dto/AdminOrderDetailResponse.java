package com.luxora.commerce.admin.dto;

import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.order.dto.OrderItemResponse;
import com.luxora.commerce.order.shipment.dto.ShipmentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminOrderDetailResponse(
        UUID id,
        UUID userId,
        String userEmail,
        String status,
        String adminNote,
        List<OrderItemResponse> items,
        ShippingAddressResponse shippingAddress,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal tax,
        BigDecimal discount,
        BigDecimal grandTotal,
        String currency,
        Instant createdAt,
        Instant updatedAt,
        AdminPaymentSummaryResponse latestPayment,
        AdminRefundSummaryResponse latestRefund,
        ShipmentResponse shipment,
        List<AdminOrderStatusHistoryResponse> statusHistory) {
}