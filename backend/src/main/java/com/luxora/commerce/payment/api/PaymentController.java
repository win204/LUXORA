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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(summary = "Create payment attempt", description = "Creates a local mock payment attempt using the persisted order total.")
    @SecurityRequirement(name = "bearerAuth")
    PaymentResponse createPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "X-Guest-Access-Token", required = false) String guestTokenHeader,
            @RequestParam(name = "token", required = false) String guestTokenParam,
            @PathVariable UUID orderId,
            @Valid @RequestBody(required = false) CreatePaymentRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        String guestToken = guestTokenHeader != null ? guestTokenHeader : guestTokenParam;
        String clientIp = servletRequest != null ? servletRequest.getRemoteAddr() : "127.0.0.1";
        return paymentService.createPayment(user, guestToken, orderId, request, clientIp);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest payment", description = "Returns the latest payment attempt for the order.")
    @SecurityRequirement(name = "bearerAuth")
    PaymentResponse latestPayment(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestHeader(name = "X-Guest-Access-Token", required = false) String guestTokenHeader,
            @RequestParam(name = "token", required = false) String guestTokenParam,
            @PathVariable UUID orderId) {
        String guestToken = guestTokenHeader != null ? guestTokenHeader : guestTokenParam;
        return paymentService.latestPayment(user, guestToken, orderId);
    }
}