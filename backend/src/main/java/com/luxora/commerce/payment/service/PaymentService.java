package com.luxora.commerce.payment.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.payment.dto.CreatePaymentRequest;
import com.luxora.commerce.payment.dto.PaymentResponse;
import com.luxora.commerce.payment.model.Payment;
import com.luxora.commerce.payment.model.PaymentStatus;
import com.luxora.commerce.payment.provider.PaymentProvider;
import com.luxora.commerce.payment.repository.PaymentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final String DEFAULT_MOCK_OUTCOME = "SUCCEEDED";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            PaymentProvider paymentProvider,
            PaymentMapper paymentMapper) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public PaymentResponse createPayment(AuthenticatedUser user, UUID orderId, CreatePaymentRequest request) {
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, user.id())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        if (order.getStatus() == OrderStatus.PAID || paymentRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.SUCCEEDED)) {
            throw new ConflictException("ORDER_ALREADY_PAID", "Order is already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("ORDER_CANCELLED", "Cancelled orders cannot be paid");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("ORDER_PAYMENT_INVALID", "Only pending orders can be paid");
        }

        String outcome = normalizeOutcome(request);
        PaymentProvider.PaymentProviderResult providerResult = paymentProvider.charge(new PaymentProvider.PaymentProviderRequest(
                order.getId().toString(),
                order.getGrandTotal(),
                order.getCurrency(),
                outcome));
        Payment payment = new Payment(
                order,
                paymentProvider.providerName(),
                providerResult.providerReference(),
                order.getGrandTotal(),
                order.getCurrency());

        if (providerResult.succeeded()) {
            payment.markSucceeded();
            order.markPaid();
        } else {
            payment.markFailed();
        }

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse latestPayment(AuthenticatedUser user, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, user.id())
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        Payment payment = paymentRepository.findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));
        return paymentMapper.toResponse(payment);
    }

    private String normalizeOutcome(CreatePaymentRequest request) {
        if (request == null || request.mockOutcome() == null || request.mockOutcome().isBlank()) {
            return DEFAULT_MOCK_OUTCOME;
        }
        return request.mockOutcome().trim().toUpperCase();
    }
}