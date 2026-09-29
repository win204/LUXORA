package com.luxora.commerce.payment.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.payment.dto.CreatePaymentRequest;
import com.luxora.commerce.payment.dto.PaymentResponse;
import com.luxora.commerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/payments")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create payment attempt", description = "Creates a local mock payment attempt for the authenticated owner's order using the persisted order total.")
    @SecurityRequirement(name = "bearerAuth")
    PaymentResponse createPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) CreatePaymentRequest request) {
        return paymentService.createPayment(user, orderId, request);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest payment", description = "Returns the latest payment attempt for the authenticated owner's order.")
    @SecurityRequirement(name = "bearerAuth")
    PaymentResponse latestPayment(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID orderId) {
        return paymentService.latestPayment(user, orderId);
    }
}