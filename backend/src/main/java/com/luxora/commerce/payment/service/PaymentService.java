package com.luxora.commerce.payment.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.common.exception.ConflictException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.common.exception.UnauthorizedException;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderStatus;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.payment.dto.CreatePaymentRequest;
import com.luxora.commerce.payment.dto.PaymentResponse;
import com.luxora.commerce.payment.model.Payment;
import com.luxora.commerce.payment.model.PaymentStatus;
import com.luxora.commerce.payment.provider.PaymentProvider;
import com.luxora.commerce.payment.provider.VnPayPaymentProvider;
import com.luxora.commerce.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentService {

    private static final String DEFAULT_MOCK_OUTCOME = "SUCCEEDED";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProvider mockPaymentProvider;
    private final VnPayPaymentProvider vnPayPaymentProvider;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            @Qualifier("mockPaymentProvider") PaymentProvider mockPaymentProvider,
            VnPayPaymentProvider vnPayPaymentProvider,
            PaymentMapper paymentMapper) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.mockPaymentProvider = mockPaymentProvider;
        this.vnPayPaymentProvider = vnPayPaymentProvider;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public PaymentResponse createPayment(AuthenticatedUser user, String guestToken, UUID orderId, CreatePaymentRequest request, String clientIp) {
        Order order = getAuthorizedOrder(orderId, user, guestToken, true);
        if (order.getStatus() == OrderStatus.PAID || paymentRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.SUCCEEDED)) {
            throw new ConflictException("ORDER_ALREADY_PAID", "Order is already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("ORDER_CANCELLED", "Cancelled orders cannot be paid");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("ORDER_PAYMENT_INVALID", "Only pending orders can be paid");
        }

        String provider = (request != null && "VNPAY".equalsIgnoreCase(request.provider())) ? VnPayPaymentProvider.PROVIDER : "MOCK";

        if (VnPayPaymentProvider.PROVIDER.equals(provider)) {
            PaymentProvider.PaymentProviderResult providerResult = vnPayPaymentProvider.charge(new PaymentProvider.PaymentProviderRequest(
                    order.getId().toString(),
                    order.getGrandTotal(),
                    order.getCurrency(),
                    null,
                    request != null ? request.bankCode() : null,
                    clientIp,
                    request != null ? request.returnUrl() : null));

            Payment payment = new Payment(
                    order,
                    VnPayPaymentProvider.PROVIDER,
                    providerResult.providerReference(),
                    order.getGrandTotal(),
                    order.getCurrency());

            return paymentMapper.toResponse(paymentRepository.save(payment), providerResult.paymentUrl());
        }

        String outcome = normalizeOutcome(request);
        PaymentProvider.PaymentProviderResult providerResult = mockPaymentProvider.charge(new PaymentProvider.PaymentProviderRequest(
                order.getId().toString(),
                order.getGrandTotal(),
                order.getCurrency(),
                outcome));
        Payment payment = new Payment(
                order,
                mockPaymentProvider.providerName(),
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

    @Transactional
    public PaymentResponse createPayment(AuthenticatedUser user, String guestToken, UUID orderId, CreatePaymentRequest request) {
        return createPayment(user, guestToken, orderId, request, null);
    }

    @Transactional
    public PaymentResponse createPayment(AuthenticatedUser user, UUID orderId, CreatePaymentRequest request) {
        return createPayment(user, null, orderId, request, null);
    }

    @Transactional(readOnly = true)
    public PaymentResponse latestPayment(AuthenticatedUser user, String guestToken, UUID orderId) {
        Order order = getAuthorizedOrder(orderId, user, guestToken, false);
        Payment payment = paymentRepository.findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElseThrow(() -> new NotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));
        return paymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse latestPayment(AuthenticatedUser user, UUID orderId) {
        return latestPayment(user, null, orderId);
    }

    @Transactional
    public Map<String, String> processVnPayIpn(Map<String, String> params) {
        if (!vnPayPaymentProvider.verifySignature(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid Checksum");
        }

        String txnRef = params.get("vnp_TxnRef");
        if (!StringUtils.hasText(txnRef)) {
            return Map.of("RspCode", "01", "Message", "Order Not Found");
        }

        Optional<Payment> paymentOpt = paymentRepository.findByProviderReference(txnRef);
        Order order;
        Payment payment;
        if (paymentOpt.isPresent()) {
            payment = paymentOpt.get();
            order = orderRepository.findWithItemsByIdForUpdate(payment.getOrder().getId()).orElse(null);
        } else {
            UUID orderId = extractOrderIdFromTxnRef(txnRef);
            if (orderId == null) {
                return Map.of("RspCode", "01", "Message", "Order Not Found");
            }
            order = orderRepository.findWithItemsByIdForUpdate(orderId).orElse(null);
            payment = (order != null)
                    ? new Payment(order, VnPayPaymentProvider.PROVIDER, txnRef, order.getGrandTotal(), order.getCurrency())
                    : null;
        }

        if (order == null) {
            return Map.of("RspCode", "01", "Message", "Order Not Found");
        }

        long expectedCents = order.getGrandTotal().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        String vnpAmountStr = params.get("vnp_Amount");
        long vnpAmount;
        try {
            vnpAmount = Long.parseLong(vnpAmountStr);
        } catch (Exception ex) {
            return Map.of("RspCode", "04", "Message", "Invalid Amount");
        }

        if (expectedCents != vnpAmount) {
            return Map.of("RspCode", "04", "Message", "Invalid Amount");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            return Map.of("RspCode", "02", "Message", "Order already confirmed");
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        boolean succeeded = "00".equals(responseCode) && ("00".equals(transactionStatus) || transactionStatus == null);

        if (succeeded) {
            payment.markSucceeded();
            order.markPaid();
        } else {
            payment.markFailed();
        }
        paymentRepository.save(payment);

        return Map.of("RspCode", "00", "Message", "Confirm Success");
    }

    @Transactional
    public Map<String, Object> verifyVnPayReturn(Map<String, String> params) {
        boolean validSignature = vnPayPaymentProvider.verifySignature(params);
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        boolean succeeded = validSignature && "00".equals(responseCode);

        if (succeeded && StringUtils.hasText(txnRef)) {
            processVnPayIpn(params);
        }

        return Map.of(
                "validSignature", validSignature,
                "succeeded", succeeded,
                "responseCode", responseCode != null ? responseCode : "",
                "txnRef", txnRef != null ? txnRef : "");
    }

    private UUID extractOrderIdFromTxnRef(String txnRef) {
        try {
            String[] parts = txnRef.split("_");
            if (parts.length >= 2) {
                return UUID.fromString(parts[1]);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Order getAuthorizedOrder(UUID orderId, AuthenticatedUser user, String guestToken, boolean forUpdate) {
        if (user == null && !StringUtils.hasText(guestToken)) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication or guest token is required");
        }
        Order order = (forUpdate ? orderRepository.findWithItemsByIdForUpdate(orderId) : orderRepository.findWithItemsById(orderId))
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        if (order.isGuestOrder()) {
            if (!StringUtils.hasText(guestToken) || !guestToken.equals(order.getGuestAccessToken())) {
                throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
            }
        } else {
            if (user == null || !order.getUser().getId().equals(user.id())) {
                throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
            }
        }
        return order;
    }

    private String normalizeOutcome(CreatePaymentRequest request) {
        if (request == null || request.mockOutcome() == null || request.mockOutcome().isBlank()) {
            return DEFAULT_MOCK_OUTCOME;
        }
        return request.mockOutcome().trim().toUpperCase();
    }
}