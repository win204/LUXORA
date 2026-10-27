package com.luxora.commerce.order.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.cart.service.CartService;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.checkout.dto.CheckoutPreviewRequest;
import com.luxora.commerce.checkout.service.CheckoutCalculation;
import com.luxora.commerce.checkout.service.CheckoutCalculationItem;
import com.luxora.commerce.checkout.service.CheckoutCalculationService;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import com.luxora.commerce.common.exception.UnauthorizedException;
import com.luxora.commerce.order.dto.OrderResponse;
import com.luxora.commerce.order.dto.OrderSummaryResponse;
import com.luxora.commerce.order.model.Order;
import com.luxora.commerce.order.model.OrderItem;
import com.luxora.commerce.order.repository.OrderRepository;
import com.luxora.commerce.promotion.service.PromotionService;
import com.luxora.commerce.user.model.User;
import com.luxora.commerce.user.repository.UserRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.luxora.commerce.aws.ses.EmailService;
import com.luxora.commerce.loyalty.service.LoyaltyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

    private final CheckoutCalculationService calculator;
    private final OrderRepository orders;
    private final UserRepository users;
    private final CartService carts;
    private final OrderMapper mapper;
    private final PromotionService promotions;
    private final LoyaltyService loyaltyService;
    private final com.luxora.commerce.flashsale.service.FlashSaleService flashSaleService;
    private final EmailService emailService;

    public OrderService(
            CheckoutCalculationService calculator,
            OrderRepository orders,
            UserRepository users,
            CartService carts,
            OrderMapper mapper,
            PromotionService promotions,
            LoyaltyService loyaltyService,
            com.luxora.commerce.flashsale.service.FlashSaleService flashSaleService,
            EmailService emailService) {
        this.calculator = calculator;
        this.orders = orders;
        this.users = users;
        this.carts = carts;
        this.mapper = mapper;
        this.promotions = promotions;
        this.loyaltyService = loyaltyService;
        this.flashSaleService = flashSaleService;
        this.emailService = emailService;
    }

    @Transactional
    public OrderResponse createOrder(AuthenticatedUser user, String cartId, CheckoutPreviewRequest request) {
        boolean isGuest = (user == null);
        if (isGuest) {
            if (!StringUtils.hasText(request.email())) {
                throw new BadRequestException("EMAIL_REQUIRED", "Email is required for guest checkout");
            }
            if (!StringUtils.hasText(cartId)) {
                throw new BadRequestException("CART_ID_REQUIRED", "cartId is required for guest checkout");
            }
        }

        CheckoutCalculation c = calculator.calculate(user, cartId, request, true);

        User owner = isGuest ? null : users.getReferenceById(user.id());
        String customerEmail = isGuest
                ? request.email().trim().toLowerCase()
                : (StringUtils.hasText(request.email()) ? request.email().trim().toLowerCase() : owner.getEmail().toLowerCase());
        String guestAccessToken = isGuest ? UUID.randomUUID().toString().replace("-", "") : null;

        Order order = new Order(
                owner,
                customerEmail,
                guestAccessToken,
                c.shippingAddress(),
                c.subtotal(),
                c.shippingFee(),
                c.tax(),
                c.discount(),
                c.grandTotal(),
                c.currency(),
                c.promotion());

        if (c.loyaltyPointsRedeemed() != null && c.loyaltyPointsRedeemed() > 0) {
            order.setLoyaltyPointsRedeemed(c.loyaltyPointsRedeemed());
            order.setLoyaltyDiscount(c.loyaltyDiscount());
        }

        for (CheckoutCalculationItem i : c.items()) {
            i.variant().getInventoryItem().decrement(i.quantity());
            if (i.flashSaleItemId() != null && flashSaleService != null) {
                flashSaleService.recordSale(i.flashSaleItemId(), i.quantity());
            }
            order.addItem(new OrderItem(
                    i.productId(),
                    i.variantId(),
                    i.slug(),
                    i.name(),
                    variantName(i),
                    i.sku(),
                    i.color(),
                    i.storage(),
                    i.imageUrl(),
                    i.unitPrice(),
                    i.quantity(),
                    i.lineTotal()));
        }

        promotions.consume(c.promotion());
        Order saved = orders.save(order);

        if (!isGuest && c.loyaltyPointsRedeemed() != null && c.loyaltyPointsRedeemed() > 0) {
            loyaltyService.redeemPointsForOrder(user.id(), c.loyaltyPointsRedeemed(), saved, c.loyaltyDiscount());
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (isGuest) {
                        carts.clearAnonymousCart(cartId);
                    } else {
                        carts.clearAuthenticatedCart(user.id());
                    }
                    if (emailService != null) {
                        emailService.sendOrderConfirmation(saved);
                    }
                }
            });
        } else {
            if (isGuest) {
                carts.clearAnonymousCart(cartId);
            } else {
                carts.clearAuthenticatedCart(user.id());
            }
            if (emailService != null) {
                emailService.sendOrderConfirmation(saved);
            }
        }

        return mapper.toResponse(saved);
    }

    @Transactional
    public OrderResponse createOrder(AuthenticatedUser user, CheckoutPreviewRequest request) {
        return createOrder(user, null, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listOrders(AuthenticatedUser u, int page, int size) {
        Page<Order> p = orders.findPageByUserId(u.id(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<UUID> ids = p.getContent().stream().map(Order::getId).toList();
        if (ids.isEmpty()) {
            return new PageResponse<>(List.of(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
        Map<UUID, Order> loaded = orders.findAllWithItemsByUserIdAndIdIn(u.id(), ids).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        return new PageResponse<>(
                p.getContent().stream().map(o -> loaded.getOrDefault(o.getId(), o)).map(mapper::toSummaryResponse).toList(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(AuthenticatedUser u, String guestToken, UUID id) {
        if (u == null && !StringUtils.hasText(guestToken)) {
            throw new UnauthorizedException("UNAUTHORIZED", "Authentication or guest token is required");
        }
        Order order = orders.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));

        if (order.isGuestOrder()) {
            if (!StringUtils.hasText(guestToken) || !guestToken.equals(order.getGuestAccessToken())) {
                throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
            }
        } else {
            if (u == null || !order.getUser().getId().equals(u.id())) {
                throw new NotFoundException("ORDER_NOT_FOUND", "Order not found");
            }
        }
        return mapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(AuthenticatedUser u, UUID id) {
        return getOrder(u, null, id);
    }

    private String variantName(CheckoutCalculationItem i) {
        if (i.color() == null && i.storage() == null) {
            return i.sku();
        }
        if (i.color() == null) {
            return i.storage();
        }
        if (i.storage() == null) {
            return i.color();
        }
        return i.color() + " / " + i.storage();
    }
}