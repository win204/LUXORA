package com.luxora.commerce.order.service;

import com.luxora.commerce.checkout.dto.ShippingAddressResponse;
import com.luxora.commerce.order.dto.OrderItemResponse;
import com.luxora.commerce.order.dto.OrderResponse;
import com.luxora.commerce.order.dto.OrderSummaryItemResponse;
import com.luxora.commerce.order.dto.OrderSummaryResponse;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.shipment.dto.ShipmentResponse;
import com.luxora.commerce.order.shipment.model.Shipment;
import org.springframework.stereotype.Component;

@Component
class OrderMapper {

    OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                new ShippingAddressResponse(
                        order.getRecipientName(),
                        order.getPhone(),
                        order.getAddressLine1(),
                        order.getAddressLine2(),
                        order.getCity(),
                        order.getProvince(),
                        order.getCountry(),
                        order.getPostalCode()),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTax(),
                order.getDiscount(),
                order.getGrandTotal(),
                order.getCurrency(),
                order.getCreatedAt(),
                toShipmentResponse(order.getShipment()));
    }

    OrderSummaryResponse toSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus().name(),
                order.getGrandTotal(),
                order.getCurrency(),
                order.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                order.getItems().stream().limit(3).map(this::toSummaryItemResponse).toList(),
                order.getCreatedAt());
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        return new ShipmentResponse(
                shipment.getCarrier(),
                shipment.getTrackingNumber(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt());
    }
    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getVariantId(),
                item.getProductSlug(),
                item.getProductName(),
                item.getVariantName(),
                item.getSku(),
                item.getColor(),
                item.getStorage(),
                item.getImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal());
    }

    private OrderSummaryItemResponse toSummaryItemResponse(OrderItem item) {
        return new OrderSummaryItemResponse(
                item.getProductId(),
                item.getVariantId(),
                item.getProductName(),
                item.getVariantName(),
                item.getSku(),
                item.getQuantity());
    }
}
