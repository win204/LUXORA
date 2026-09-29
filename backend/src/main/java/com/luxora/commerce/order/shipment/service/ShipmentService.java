package com.luxora.commerce.order.shipment.service;

import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.model.OrderStatusHistory;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.order.repository.OrderStatusHistoryRepository;
import com.luxora.commerce.order.shipment.dto.ShipmentRequest;
import com.luxora.commerce.order.shipment.model.Shipment;
import com.luxora.commerce.order.shipment.repository.ShipmentRepository;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;

    public ShipmentService(
            OrderRepository orderRepository,
            ShipmentRepository shipmentRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order createShipment(UUID orderId, UUID adminUserId, ShipmentRequest request) {
        Order order = findLockedOrder(orderId);
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new ConflictException("ORDER_SHIPMENT_INVALID_STATUS", "Shipment can only be created for processing orders");
        }
        if (shipmentRepository.existsByOrder_Id(order.getId())) {
            throw new ConflictException("ORDER_SHIPMENT_EXISTS", "Order already has a shipment");
        }

        Shipment shipment = new Shipment(order, trim(request.carrier()), trim(request.trackingNumber()), Instant.now());
        order.attachShipment(shipment);
        order.changeStatus(OrderStatus.SHIPPED);
        shipmentRepository.save(shipment);
        appendHistory(order, OrderStatus.PROCESSING, OrderStatus.SHIPPED, adminUserId);
        return order;
    }

    @Transactional
    public Order updateShipment(UUID orderId, ShipmentRequest request) {
        Order order = findLockedOrder(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ConflictException("ORDER_SHIPMENT_UPDATE_INVALID_STATUS", "Shipment can only be updated while order is shipped");
        }
        Shipment shipment = shipmentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new NotFoundException("ORDER_SHIPMENT_NOT_FOUND", "Shipment not found"));
        shipment.updateTracking(trim(request.carrier()), trim(request.trackingNumber()));
        order.attachShipment(shipment);
        return order;
    }

    @Transactional
    public Order markDelivered(UUID orderId, UUID adminUserId) {
        Order order = findLockedOrder(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ConflictException("ORDER_STATUS_TRANSITION_INVALID", "Order status transition is not allowed");
        }
        Shipment shipment = shipmentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new ConflictException("ORDER_SHIPMENT_REQUIRED", "Shipment metadata is required before delivery"));
        if (shipment.getDeliveredAt() == null) {
            shipment.markDelivered(Instant.now());
        }
        order.attachShipment(shipment);
        order.changeStatus(OrderStatus.DELIVERED);
        appendHistory(order, OrderStatus.SHIPPED, OrderStatus.DELIVERED, adminUserId);
        return order;
    }

    private Order findLockedOrder(UUID orderId) {
        return orderRepository.findAdminByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }

    private void appendHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, UUID adminUserId) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Admin user not found"));
        orderStatusHistoryRepository.save(new OrderStatusHistory(order, fromStatus, toStatus, adminUser));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
