package com.luxora.commerce.returning.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.inventory.model.InventoryItem;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.payment.model.Payment;
import com.luxora.commerce.payment.model.PaymentStatus;
import com.luxora.commerce.payment.repository.PaymentRepository;
import com.luxora.commerce.refund.model.Refund;
import com.luxora.commerce.refund.model.RefundStatus;
import com.luxora.commerce.refund.provider.RefundProvider;
import com.luxora.commerce.refund.repository.RefundRepository;
import com.luxora.commerce.returning.dto.AdminApproveReturnRequest;
import com.luxora.commerce.returning.dto.AdminReturnNoteRequest;
import com.luxora.commerce.returning.dto.AdminReceiveReturnRequest;
import com.luxora.commerce.returning.dto.AdminRejectReturnRequest;
import com.luxora.commerce.returning.dto.AdminReturnQuantityRequest;
import com.luxora.commerce.returning.dto.CreateReturnItemRequest;
import com.luxora.commerce.returning.dto.CreateReturnRequest;
import com.luxora.commerce.returning.dto.ReturnRefundRequest;
import com.luxora.commerce.returning.dto.ReturnResponse;
import com.luxora.commerce.returning.dto.ReturnSummaryResponse;
import com.luxora.commerce.returning.model.ReturnItem;
import com.luxora.commerce.returning.model.ReturnShipment;
import com.luxora.commerce.returning.model.ReturnRequest;
import com.luxora.commerce.returning.model.ReturnStatus;
import com.luxora.commerce.returning.model.ReturnStatusHistory;
import com.luxora.commerce.returning.provider.ReturnShippingProvider;
import com.luxora.commerce.returning.repository.ReturnRepository;
import com.luxora.commerce.returning.repository.ReturnShipmentRepository;
import com.luxora.commerce.returning.repository.ReturnStatusHistoryRepository;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {

    private static final String DEFAULT_MOCK_OUTCOME = "SUCCEEDED";
    private static final Collection<ReturnStatus> ACTIVE_QUANTITY_STATUSES = EnumSet.of(
            ReturnStatus.REQUESTED,
            ReturnStatus.APPROVED,
            ReturnStatus.RECEIVED,
            ReturnStatus.REFUNDED);

    private final OrderRepository orderRepository;
    private final ReturnRepository returnRepository;
    private final ReturnStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final RefundProvider refundProvider;
    private final ReturnShippingProvider returnShippingProvider;
    private final ReturnShipmentRepository returnShipmentRepository;
    private final ReturnMapper returnMapper;
    private final Duration returnWindow;

    public ReturnService(
            OrderRepository orderRepository,
            ReturnRepository returnRepository,
            ReturnStatusHistoryRepository historyRepository,
            UserRepository userRepository,
            ProductVariantRepository productVariantRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            RefundProvider refundProvider,
            ReturnShippingProvider returnShippingProvider,
            ReturnShipmentRepository returnShipmentRepository,
            ReturnMapper returnMapper,
            @Value("${luxora.returns.window:P14D}") Duration returnWindow) {
        this.orderRepository = orderRepository;
        this.returnRepository = returnRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.productVariantRepository = productVariantRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.refundProvider = refundProvider;
        this.returnShippingProvider = returnShippingProvider;
        this.returnShipmentRepository = returnShipmentRepository;
        this.returnMapper = returnMapper;
        this.returnWindow = returnWindow;
    }

    @Transactional
    public ReturnResponse createReturn(AuthenticatedUser user, UUID orderId, CreateReturnRequest request) {
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, user.id())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        validateEligibility(order);

        Map<UUID, OrderItem> orderItemsById = order.getItems().stream().collect(Collectors.toMap(OrderItem::getId, Function.identity()));
        Map<UUID, Integer> requestedQuantities = request.items().stream()
                .collect(Collectors.groupingBy(CreateReturnItemRequest::orderItemId, Collectors.summingInt(CreateReturnItemRequest::quantity)));
        Map<UUID, Integer> alreadyReturned = activeQuantities(requestedQuantities.keySet());

        User owner = userRepository.getReferenceById(user.id());
        ReturnRequest returnRequest = new ReturnRequest(order, owner, trimToNull(request.customerNote()));
        for (CreateReturnItemRequest itemRequest : request.items()) {
            OrderItem orderItem = orderItemsById.get(itemRequest.orderItemId());
            if (orderItem == null) {
                throw new NotFoundException("ORDER_ITEM_NOT_FOUND", "Order item not found");
            }
            int totalRequestedForItem = requestedQuantities.get(orderItem.getId());
            int remaining = orderItem.getQuantity() - alreadyReturned.getOrDefault(orderItem.getId(), 0);
            if (totalRequestedForItem > remaining) {
                throw new ConflictException("RETURN_QUANTITY_EXCEEDED", "Return quantity exceeds purchased quantity");
            }
            returnRequest.addItem(new ReturnItem(orderItem, itemRequest.quantity(), trim(itemRequest.reason())));
        }

        ReturnRequest saved = returnRepository.save(returnRequest);
        historyRepository.save(new ReturnStatusHistory(saved, ReturnStatus.REQUESTED, ReturnStatus.REQUESTED, owner));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnSummaryResponse> listReturns(AuthenticatedUser user, int page, int size) {
        Page<ReturnRequest> returnPage = returnRepository.findPageByUserId(user.id(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestedAt")));
        List<ReturnSummaryResponse> content = returnPage.getContent().stream().map(returnMapper::toSummary).toList();
        return new PageResponse<>(content, returnPage.getNumber(), returnPage.getSize(), returnPage.getTotalElements(), returnPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ReturnResponse getReturn(AuthenticatedUser user, UUID returnId) {
        ReturnRequest request = returnRepository.findByIdAndUserId(returnId, user.id())
                .orElseThrow(() -> new NotFoundException("RETURN_NOT_FOUND", "Return not found"));
        return toResponse(request);
    }

    @Transactional
    public ReturnResponse cancelReturn(AuthenticatedUser user, UUID returnId) {
        ReturnRequest request = returnRepository.findByIdAndUserIdForUpdate(returnId, user.id())
                .orElseThrow(() -> new NotFoundException("RETURN_NOT_FOUND", "Return not found"));
        if (request.getStatus() == ReturnStatus.CANCELLED) {
            return toResponse(request);
        }
        if (request.getStatus() != ReturnStatus.REQUESTED && request.getStatus() != ReturnStatus.APPROVED) {
            throw new ConflictException("RETURN_CANCEL_INVALID", "Only requested or approved returns can be cancelled");
        }
        ReturnStatus from = request.getStatus();
        request.cancel();
        historyRepository.save(new ReturnStatusHistory(request, from, ReturnStatus.CANCELLED, userRepository.getReferenceById(user.id())));
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReturnSummaryResponse> listAdminReturns(
            int page,
            int size,
            ReturnStatus status,
            UUID orderId,
            String customerEmail,
            String trackingNumber,
            Instant dateFrom,
            Instant dateTo,
            String sort) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("RETURN_DATE_RANGE_INVALID", "dateFrom must be before or equal to dateTo");
        }
        Page<ReturnRequest> returnPage = returnRepository.findAdminPage(
                status,
                orderId,
                trimToNull(customerEmail),
                trimToNull(trackingNumber),
                dateFrom,
                dateTo,
                PageRequest.of(page, size, parseAdminSort(sort)));
        List<UUID> ids = returnPage.getContent().stream().map(ReturnRequest::getId).toList();
        Map<UUID, ReturnRequest> requestsById = ids.isEmpty()
                ? Map.of()
                : returnRepository.findAdminSummariesByIdIn(ids).stream()
                        .collect(Collectors.toMap(ReturnRequest::getId, Function.identity()));
        List<ReturnSummaryResponse> content = ids.stream().map(requestsById::get).map(returnMapper::toSummary).toList();
        return new PageResponse<>(content, returnPage.getNumber(), returnPage.getSize(), returnPage.getTotalElements(), returnPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ReturnResponse getAdminReturn(UUID returnId) {
        ReturnRequest request = returnRepository.findAdminById(returnId)
                .orElseThrow(() -> new NotFoundException("RETURN_NOT_FOUND", "Return not found"));
        return toResponse(request);
    }

    @Transactional
    public ReturnResponse updateAdminNote(UUID returnId, AdminReturnNoteRequest request) {
        ReturnRequest returnRequest = lockedAdminReturn(returnId);
        returnRequest.updateAdminNote(trimToNull(request.note()));
        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnResponse approve(UUID returnId, UUID adminUserId, AdminApproveReturnRequest request) {
        ReturnRequest returnRequest = lockedAdminReturn(returnId);
        if (returnRequest.getStatus() == ReturnStatus.APPROVED) {
            return toResponse(returnRequest);
        }
        if (returnRequest.getStatus() != ReturnStatus.REQUESTED) {
            throw new ConflictException("RETURN_APPROVE_INVALID", "Only requested returns can be approved");
        }
        applyQuantities(returnRequest, request == null ? null : request.items(), true);
        if (returnRequest.getItems().stream().mapToInt(ReturnItem::getApprovedQuantity).sum() <= 0) {
            throw new ConflictException("RETURN_APPROVE_EMPTY", "Approved return quantity must be greater than zero");
        }
        ReturnStatus from = returnRequest.getStatus();
        returnRequest.approve(trimToNull(request == null ? null : request.adminNote()));
        historyRepository.save(new ReturnStatusHistory(returnRequest, from, ReturnStatus.APPROVED, userRepository.getReferenceById(adminUserId)));
        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnResponse reject(UUID returnId, UUID adminUserId, AdminRejectReturnRequest request) {
        ReturnRequest returnRequest = lockedAdminReturn(returnId);
        if (returnRequest.getStatus() == ReturnStatus.REJECTED) {
            return toResponse(returnRequest);
        }
        if (returnRequest.getStatus() != ReturnStatus.REQUESTED) {
            throw new ConflictException("RETURN_REJECT_INVALID", "Only requested returns can be rejected");
        }
        ReturnStatus from = returnRequest.getStatus();
        returnRequest.reject(trim(request.adminNote()));
        historyRepository.save(new ReturnStatusHistory(returnRequest, from, ReturnStatus.REJECTED, userRepository.getReferenceById(adminUserId)));
        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnResponse receive(UUID returnId, UUID adminUserId, AdminReceiveReturnRequest request) {
        ReturnRequest returnRequest = lockedAdminReturn(returnId);
        if (returnRequest.getStatus() == ReturnStatus.RECEIVED || returnRequest.getStatus() == ReturnStatus.REFUNDED) {
            return toResponse(returnRequest);
        }
        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
            throw new ConflictException("RETURN_RECEIVE_INVALID", "Only approved returns can be received");
        }
        ReturnShipment shipment = returnShipmentRepository.findByReturnRequest_IdForUpdate(returnId)
                .orElseThrow(() -> new ConflictException("RETURN_SHIPMENT_REQUIRED", "Return shipment is required before receive"));
        applyQuantities(returnRequest, request == null ? null : request.items(), false);
        if (returnRequest.getItems().stream().mapToInt(ReturnItem::getReceivedQuantity).sum() <= 0) {
            throw new ConflictException("RETURN_RECEIVE_EMPTY", "Received return quantity must be greater than zero");
        }
        restock(returnRequest.getItems());
        shipment.markReceived();
        ReturnStatus from = returnRequest.getStatus();
        returnRequest.markReceived(trimToNull(request == null ? null : request.adminNote()));
        historyRepository.save(new ReturnStatusHistory(returnRequest, from, ReturnStatus.RECEIVED, userRepository.getReferenceById(adminUserId)));
        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnResponse generateShippingLabel(UUID returnId) {
        ReturnRequest returnRequest = lockedAdminReturn(returnId);
        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
            throw new ConflictException("RETURN_LABEL_INVALID", "Return label can be generated only for approved returns");
        }
        if (returnRequest.getShipment() != null) {
            return toResponse(returnRequest);
        }
        ReturnShippingProvider.ReturnShippingLabel label = returnShippingProvider.createLabel(returnId);
        ReturnShipment shipment = new ReturnShipment(returnRequest, label.carrier(), label.trackingNumber(), label.mockLabelReference());
        returnRequest.attachShipment(shipment);
        returnShipmentRepository.save(shipment);
        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnResponse markShipped(AuthenticatedUser user, UUID returnId) {
        ReturnRequest returnRequest = returnRepository.findByIdAndUserIdForUpdate(returnId, user.id())
                .orElseThrow(() -> new NotFoundException("RETURN_NOT_FOUND", "Return not found"));
        if (returnRequest.getStatus() != ReturnStatus.APPROVED) {
            throw new ConflictException("RETURN_SHIP_INVALID", "Only approved returns can be marked shipped");
        }
        ReturnShipment shipment = returnShipmentRepository.findByReturnRequest_IdForUpdate(returnId)
                .orElseThrow(() -> new ConflictException("RETURN_LABEL_REQUIRED", "Return shipping label is required before shipment"));
        shipment.markShipped();
        return toResponse(returnRequest);
    }

    @Transactional
    public ReturnResponse markReceived(UUID returnId, UUID adminUserId, AdminReceiveReturnRequest request) {
        return receive(returnId, adminUserId, request);
    }
    @Transactional
    public ReturnResponse refund(UUID returnId, UUID adminUserId, ReturnRefundRequest request) {
        ReturnRequest returnRequest = lockedAdminReturn(returnId);
        if (returnRequest.getStatus() == ReturnStatus.REFUNDED || refundRepository.existsByReturnRequest_IdAndStatus(returnId, RefundStatus.SUCCEEDED)) {
            if (returnRequest.getStatus() != ReturnStatus.REFUNDED) {
                ReturnStatus from = returnRequest.getStatus();
                returnRequest.markRefunded();
                historyRepository.save(new ReturnStatusHistory(returnRequest, from, ReturnStatus.REFUNDED, userRepository.getReferenceById(adminUserId)));
            }
            return toResponse(returnRequest);
        }
        if (returnRequest.getStatus() != ReturnStatus.RECEIVED) {
            throw new ConflictException("RETURN_REFUND_INVALID", "Only received returns can be refunded");
        }
        Payment payment = paymentRepository.findTopByOrder_IdAndStatusOrderByCreatedAtDesc(returnRequest.getOrder().getId(), PaymentStatus.SUCCEEDED)
                .orElseThrow(() -> new ConflictException("ORDER_PAYMENT_NOT_CAPTURED", "Order has no successful payment"));
        BigDecimal amount = receivedRefundAmount(returnRequest);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("RETURN_REFUND_EMPTY", "Refund amount must be greater than zero");
        }
        RefundProvider.RefundProviderResult providerResult = refundProvider.refund(new RefundProvider.RefundProviderRequest(
                returnRequest.getOrder().getId().toString(),
                payment.getId().toString(),
                amount,
                payment.getCurrency(),
                normalizeOutcome(request)));
        Refund refund = new Refund(
                returnRequest.getOrder(),
                payment,
                returnRequest,
                refundProvider.providerName(),
                providerResult.providerReference(),
                amount,
                payment.getCurrency(),
                trimToNull(request == null ? null : request.reason()));
        if (providerResult.succeeded()) {
            refund.markSucceeded();
            ReturnStatus from = returnRequest.getStatus();
            returnRequest.markRefunded();
            historyRepository.save(new ReturnStatusHistory(returnRequest, from, ReturnStatus.REFUNDED, userRepository.getReferenceById(adminUserId)));
        } else {
            refund.markFailed();
        }
        refundRepository.save(refund);
        return toResponse(returnRequest);
    }

    private void validateEligibility(Order order) {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ConflictException("RETURN_ORDER_NOT_ELIGIBLE", "Only delivered orders can be returned");
        }
        if (order.getShipment() == null || order.getShipment().getDeliveredAt() == null) {
            throw new ConflictException("RETURN_DELIVERY_REQUIRED", "Order delivery timestamp is required for returns");
        }
        if (order.getShipment().getDeliveredAt().plus(returnWindow).isBefore(Instant.now())) {
            throw new ConflictException("RETURN_WINDOW_EXPIRED", "Return window has expired");
        }
    }

    private ReturnRequest lockedAdminReturn(UUID returnId) {
        return returnRepository.findAdminByIdForUpdate(returnId)
                .orElseThrow(() -> new NotFoundException("RETURN_NOT_FOUND", "Return not found"));
    }

    private Map<UUID, Integer> activeQuantities(Collection<UUID> orderItemIds) {
        if (orderItemIds.isEmpty()) {
            return Map.of();
        }
        return returnRepository.activeQuantitiesByOrderItemIds(orderItemIds, ACTIVE_QUANTITY_STATUSES).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> ((Number) row[1]).intValue()));
    }

    private void applyQuantities(ReturnRequest returnRequest, List<AdminReturnQuantityRequest> quantities, boolean approval) {
        if (quantities == null || quantities.isEmpty()) {
            if (!approval) {
                returnRequest.getItems().forEach(item -> item.setReceivedQuantity(item.getApprovedQuantity()));
            }
            return;
        }
        Map<UUID, ReturnItem> itemsById = returnRequest.getItems().stream().collect(Collectors.toMap(ReturnItem::getId, Function.identity()));
        for (AdminReturnQuantityRequest quantity : quantities) {
            ReturnItem item = itemsById.get(quantity.returnItemId());
            if (item == null) {
                throw new NotFoundException("RETURN_ITEM_NOT_FOUND", "Return item not found");
            }
            if (approval) {
                if (quantity.quantity() > item.getRequestedQuantity()) {
                    throw new ConflictException("RETURN_APPROVED_QUANTITY_INVALID", "Approved quantity cannot exceed requested quantity");
                }
                item.setApprovedQuantity(quantity.quantity());
            } else {
                if (quantity.quantity() > item.getApprovedQuantity()) {
                    throw new ConflictException("RETURN_RECEIVED_QUANTITY_INVALID", "Received quantity cannot exceed approved quantity");
                }
                item.setReceivedQuantity(quantity.quantity());
            }
        }
        if (!approval) {
            for (ReturnItem item : returnRequest.getItems()) {
                if (!quantities.stream().map(AdminReturnQuantityRequest::returnItemId).collect(Collectors.toSet()).contains(item.getId())) {
                    item.setReceivedQuantity(item.getApprovedQuantity());
                }
            }
        }
    }

    private void restock(List<ReturnItem> items) {
        Map<UUID, Integer> quantitiesByVariantId = items.stream()
                .filter(item -> item.getReceivedQuantity() > 0)
                .collect(Collectors.groupingBy(item -> item.getOrderItem().getVariantId(), Collectors.summingInt(ReturnItem::getReceivedQuantity)));
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

    private BigDecimal receivedRefundAmount(ReturnRequest request) {
        return request.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getReceivedQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ReturnResponse toResponse(ReturnRequest request) {
        return returnMapper.toResponse(request, historyRepository.findByReturnRequest_IdOrderByChangedAtAsc(request.getId()));
    }

    private String normalizeOutcome(ReturnRefundRequest request) {
        if (request == null || request.mockOutcome() == null || request.mockOutcome().isBlank()) {
            return DEFAULT_MOCK_OUTCOME;
        }
        return request.mockOutcome().trim().toUpperCase(Locale.ROOT);
    }

    private Sort parseAdminSort(String sort) {
        String value = trimToNull(sort);
        if (value == null) {
            return Sort.by(Sort.Direction.DESC, "requestedAt");
        }
        String[] parts = value.split(",", -1);
        if (parts.length != 2 || !("requestedAt".equals(parts[0]) || "updatedAt".equals(parts[0]))) {
            throw new BadRequestException("RETURN_SORT_INVALID", "sort must be requestedAt,asc|desc or updatedAt,asc|desc");
        }
        try {
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("RETURN_SORT_INVALID", "sort must be requestedAt,asc|desc or updatedAt,asc|desc");
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }
}
