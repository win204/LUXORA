package com.luxora.commerce.refund.service;

import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.inventory.model.InventoryItem;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.model.OrderStatusHistory;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.order.repository.OrderStatusHistoryRepository;
import com.luxora.commerce.payment.model.Payment;
import com.luxora.commerce.payment.model.PaymentStatus;
import com.luxora.commerce.payment.repository.PaymentRepository;
import com.luxora.commerce.refund.dto.RefundAndCancelRequest;
import com.luxora.commerce.refund.model.Refund;
import com.luxora.commerce.refund.model.RefundStatus;
import com.luxora.commerce.refund.provider.RefundProvider;
import com.luxora.commerce.refund.repository.RefundRepository;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private static final String DEFAULT_MOCK_OUTCOME = "SUCCEEDED";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final RefundProvider refundProvider;
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;

    public RefundService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            RefundProvider refundProvider,
            ProductVariantRepository productVariantRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.refundProvider = refundProvider;
        this.productVariantRepository = productVariantRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order refundAndCancel(UUID orderId, UUID adminUserId, RefundAndCancelRequest request) {
        Order order = orderRepository.findAdminByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PROCESSING) {
            throw new ConflictException("ORDER_REFUND_CANCEL_INVALID", "Only paid or processing orders can be refunded and cancelled");
        }
        if (refundRepository.existsByOrder_IdAndStatus(order.getId(), RefundStatus.SUCCEEDED)) {
            throw new ConflictException("ORDER_REFUND_ALREADY_SUCCEEDED", "Order already has a successful refund");
        }

        Payment payment = paymentRepository.findTopByOrder_IdAndStatusOrderByCreatedAtDesc(order.getId(), PaymentStatus.SUCCEEDED)
                .orElseThrow(() -> new ConflictException("ORDER_PAYMENT_NOT_CAPTURED", "Order has no successful payment"));
        String outcome = normalizeOutcome(request);
        RefundProvider.RefundProviderResult providerResult = refundProvider.refund(new RefundProvider.RefundProviderRequest(
                order.getId().toString(),
                payment.getId().toString(),
                payment.getAmount(),
                payment.getCurrency(),
                outcome));
        Refund refund = new Refund(
                order,
                payment,
                refundProvider.providerName(),
                providerResult.providerReference(),
                payment.getAmount(),
                payment.getCurrency(),
                trimToNull(request == null ? null : request.reason()));

        if (providerResult.succeeded()) {
            refund.markSucceeded();
            OrderStatus fromStatus = order.getStatus();
            restock(order.getItems());
            order.markCancelled();
            User adminUser = userRepository.getReferenceById(adminUserId);
            orderStatusHistoryRepository.save(new OrderStatusHistory(order, fromStatus, OrderStatus.CANCELLED, adminUser));
        } else {
            refund.markFailed();
        }

        refundRepository.save(refund);
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

    private String normalizeOutcome(RefundAndCancelRequest request) {
        if (request == null || request.mockOutcome() == null || request.mockOutcome().isBlank()) {
            return DEFAULT_MOCK_OUTCOME;
        }
        return request.mockOutcome().trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
