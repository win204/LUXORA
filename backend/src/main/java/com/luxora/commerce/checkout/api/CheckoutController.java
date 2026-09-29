package com.luxora.commerce.checkout.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.checkout.dto.CheckoutPreviewRequest;
import com.luxora.commerce.checkout.dto.CheckoutPreviewResponse;
import com.luxora.commerce.checkout.service.CheckoutPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout")
public class CheckoutController {

    private final CheckoutPreviewService checkoutPreviewService;

    public CheckoutController(CheckoutPreviewService checkoutPreviewService) {
        this.checkoutPreviewService = checkoutPreviewService;
    }

    @PostMapping("/preview")
    @Operation(summary = "Preview checkout", description = "Returns a server-authoritative checkout preview without creating an order, reserving stock, or processing payment.")
    @SecurityRequirement(name = "bearerAuth")
    CheckoutPreviewResponse preview(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CheckoutPreviewRequest request) {
        return checkoutPreviewService.preview(user, request);
    }
}
