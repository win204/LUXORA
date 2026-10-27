package com.luxora.commerce.order.shipment.service;

import com.luxora.commerce.aws.ses.EmailService;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.common.exception.UnauthorizedException;
import com.luxora.commerce.loyalty.service.LoyaltyService;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.model.OrderStatusHistory;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.order.repository.OrderStatusHistoryRepository;
import com.luxora.commerce.order.shipment.dto.ShipmentRequest;
import com.luxora.commerce.order.shipment.model.Shipment;
import com.luxora.commerce.order.shipment.repository.ShipmentRepository;
import com.luxora.commerce.shipping.config.GhnProperties;
import com.luxora.commerce.shipping.dto.GhnWebhookPayload;
import com.luxora.commerce.shipping.provider.GhnShippingProvider;
import com.luxora.commerce.shipping.provider.MockShippingProvider;
import com.luxora.commerce.shipping.provider.ShippingOrderRequest;
import com.luxora.commerce.shipping.provider.ShippingOrderResult;
import com.luxora.commerce.shipping.provider.ShippingProvider;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final GhnShippingProvider ghnShippingProvider;
    private final MockShippingProvider mockShippingProvider;
    private final GhnProperties ghnProperties;
    private final LoyaltyService loyaltyService;
    private final EmailService emailService;

    public ShipmentService(
            OrderRepository orderRepository,
            ShipmentRepository shipmentRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            GhnShippingProvider ghnShippingProvider,
            MockShippingProvider mockShippingProvider,
            GhnProperties ghnProperties,
            LoyaltyService loyaltyService,
            EmailService emailService) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.ghnShippingProvider = ghnShippingProvider;
        this.mockShippingProvider = mockShippingProvider;
        this.ghnProperties = ghnProperties;
        this.loyaltyService = loyaltyService;
        this.emailService = emailService;
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

        String carrier;
        String trackingNumber;

        if (request.provider() != null && !request.provider().isBlank()) {
            String prov = request.provider().trim().toUpperCase();
            ShippingProvider provider = "GHN".equals(prov) ? ghnShippingProvider : mockShippingProvider;
            ShippingOrderRequest shippingOrderRequest = new ShippingOrderRequest(
                    order.getId(),
                    order.getRecipientName(),
                    order.getPhone(),
                    order.getAddressLine1(),
                    order.getAddressLine2(),
                    order.getCity(),
                    order.getProvince(),
                    order.getCountry(),
                    order.getPostalCode(),
                    500,
                    BigDecimal.ZERO,
                    order.getItems().stream().map(OrderItem::getProductName).toList());
            ShippingOrderResult result = provider.createShippingOrder(shippingOrderRequest);
            carrier = result.carrier();
            trackingNumber = result.trackingNumber();
        } else {
            carrier = trim(request.carrier());
            trackingNumber = trim(request.trackingNumber());
            if (carrier == null || carrier.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
                throw new BadRequestException("VALIDATION_ERROR", "Carrier and tracking number are required when provider is not specified");
            }
        }

        Shipment shipment = new Shipment(order, carrier, trackingNumber, Instant.now());
        order.attachShipment(shipment);
        order.changeStatus(OrderStatus.SHIPPED);
        shipmentRepository.save(shipment);
        appendHistory(order, OrderStatus.PROCESSING, OrderStatus.SHIPPED, adminUserId);

        String trackingUrl = ("GHN".equalsIgnoreCase(carrier) || (carrier != null && carrier.toUpperCase().contains("GHN")))
                ? "https://donhang.ghn.vn/?order_code=" + trackingNumber
                : "https://luxora.store/orders/" + order.getId() + (order.getGuestAccessToken() != null ? "?token=" + order.getGuestAccessToken() : "");

        if (emailService != null && order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
            emailService.sendShipmentNotification(
                    order.getCustomerEmail(),
                    order.getId().toString(),
                    carrier,
                    trackingNumber,
                    trackingUrl
            );
        }

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
        loyaltyService.awardPointsOnDelivery(order);
        return order;
    }

    @Transactional
    public void processGhnWebhook(GhnWebhookPayload payload, String tokenHeader) {
        if (ghnProperties.webhookToken() != null && !ghnProperties.webhookToken().isBlank()) {
            if (tokenHeader == null || !ghnProperties.webhookToken().equals(tokenHeader.trim())) {
                throw new UnauthorizedException("GHN_WEBHOOK_UNAUTHORIZED", "Invalid webhook token");
            }
        }
        if (payload == null || payload.orderCode() == null || payload.orderCode().isBlank()) {
            throw new BadRequestException("GHN_WEBHOOK_INVALID_PAYLOAD", "Order code is required");
        }

        Shipment shipment = shipmentRepository.findByTrackingNumber(payload.orderCode().trim())
                .orElse(null);
        if (shipment == null) {
            log.warn("GHN webhook received for unknown tracking code: {}", payload.orderCode());
            return;
        }

        Order order = findLockedOrder(shipment.getOrder().getId());
        String status = payload.status() != null ? payload.status().trim().toLowerCase() : "";

        if ("delivered".equals(status)) {
            if (order.getStatus() == OrderStatus.SHIPPED) {
                if (shipment.getDeliveredAt() == null) {
                    shipment.markDelivered(Instant.now());
                }
                order.attachShipment(shipment);
                order.changeStatus(OrderStatus.DELIVERED);
                shipmentRepository.save(shipment);
                appendHistory(order, OrderStatus.SHIPPED, OrderStatus.DELIVERED, null);
                loyaltyService.awardPointsOnDelivery(order);
                log.info("Order {} transitioned to DELIVERED via GHN webhook (tracking: {})", order.getId(), payload.orderCode());
            }
        }
    }

    private Order findLockedOrder(UUID orderId) {
        return orderRepository.findAdminByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
    }

    private void appendHistory(Order order, OrderStatus fromStatus, OrderStatus toStatus, UUID adminUserId) {
        User adminUser = adminUserId != null
                ? userRepository.findById(adminUserId).orElse(null)
                : null;
        orderStatusHistoryRepository.save(new OrderStatusHistory(order, fromStatus, toStatus, adminUser));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

