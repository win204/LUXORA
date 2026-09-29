package com.luxora.commerce.order.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.catalog.dto.PageResponse;
import com.luxora.commerce.checkout.dto.CheckoutPreviewRequest;
import com.luxora.commerce.order.dto.OrderResponse;
import com.luxora.commerce.order.dto.OrderSummaryResponse;
import com.luxora.commerce.order.service.OrderCancellationService;
import com.luxora.commerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderCancellationService orderCancellationService;

    public OrderController(OrderService orderService, OrderCancellationService orderCancellationService) {
        this.orderService = orderService;
        this.orderCancellationService = orderCancellationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create order", description = "Creates a pending order from the authenticated user's current cart without processing payment.")
    @SecurityRequirement(name = "bearerAuth")
    OrderResponse createOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckoutPreviewRequest request) {
        return orderService.createOrder(user, request);
    }

    @GetMapping
    @Operation(summary = "List orders", description = "Returns the authenticated user's order history, newest first.")
    @SecurityRequirement(name = "bearerAuth")
    PageResponse<OrderSummaryResponse> listOrders(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size from 1 to 50") @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return orderService.listOrders(user, page, size);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels a pending order owned by the authenticated user and restores inventory.")
    @SecurityRequirement(name = "bearerAuth")
    OrderResponse cancelOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return orderCancellationService.cancelCustomerOrder(user, id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order", description = "Returns a safe order snapshot for the authenticated owner.")
    @SecurityRequirement(name = "bearerAuth")
    OrderResponse getOrder(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return orderService.getOrder(user, id);
    }
}