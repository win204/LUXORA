package com.luxora.commerce.order.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.inventory.model.InventoryItem;
import com.luxora.commerce.order.dto.OrderResponse;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.model.OrderStatusHistory;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.order.repository.OrderStatusHistoryRepository;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    public OrderCancellationService(
            OrderRepository orderRepository,
            ProductVariantRepository productVariantRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResponse cancelCustomerOrder(AuthenticatedUser authenticatedUser, UUID orderId) {
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, authenticatedUser.id())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        return orderMapper.toResponse(cancelLockedOrder(order, authenticatedUser.id()));
    }

    @Transactional
    public Order cancelAdminOrder(UUID orderId, UUID adminUserId) {
        Order order = orderRepository.findAdminByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        return cancelLockedOrder(order, adminUserId);
    }

    private Order cancelLockedOrder(Order order, UUID actorUserId) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("ORDER_CANCELLATION_INVALID", "Only pending orders can be cancelled");
        }

        restock(order.getItems());
        OrderStatus fromStatus = order.getStatus();
        order.markCancelled();
        User actor = userRepository.getReferenceById(actorUserId);
        orderStatusHistoryRepository.save(new OrderStatusHistory(order, fromStatus, OrderStatus.CANCELLED, actor));
        return order;
    }

    private void restock(List<OrderItem> items) {
        Map<UUID, Integer> quantitiesByVariantId = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getVariantId, Collectors.summingInt(OrderItem::getQuantity)));
        if (quantitiesByVariantId.isEmpty()) {
            return;
        }

        Map<UUID, ProductVariant> variantsById = productVariantRepository.findAllByIdInForUpdate(quantitiesByVariantId.keySet()).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        for (Map.Entry<UUID, Integer> entry : quantitiesByVariantId.entrySet()) {
            ProductVariant variant = variantsById.get(entry.getKey());
            if (variant == null || variant.getInventoryItem() == null) {
                throw new NotFoundException("VARIANT_NOT_FOUND", "Product variant not found");
            }
            InventoryItem inventory = variant.getInventoryItem();
            inventory.restock(entry.getValue());
        }
    }
}