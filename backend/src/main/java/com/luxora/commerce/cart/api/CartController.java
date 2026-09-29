package com.luxora.commerce.cart.api;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.cart.dto.AddCartItemRequest;
import com.luxora.commerce.cart.dto.CartResponse;
import com.luxora.commerce.cart.dto.UpdateCartItemRequest;
import com.luxora.commerce.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Anonymous and authenticated Redis-backed cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get cart", description = "Returns the authenticated user's cart or an anonymous cart by cartId.")
    CartResponse getCart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String cartId,
            @RequestHeader(name = "X-Cart-Id", required = false) String cartIdHeader) {
        return cartService.getCart(user, resolveCartId(cartId, cartIdHeader));
    }

    @PostMapping("/items")
    @Operation(summary = "Add cart item", description = "Adds a product variant using backend price and stock.")
    CartResponse addItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddCartItemRequest request,
            @RequestHeader(name = "X-Cart-Id", required = false) String cartIdHeader) {
        return cartService.addItem(user, resolveCartId(request.cartId(), cartIdHeader), request.variantId(), request.quantity());
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update cart item", description = "Updates quantity for a cart item.")
    CartResponse updateItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader(name = "X-Cart-Id", required = false) String cartIdHeader) {
        return cartService.updateItem(user, resolveCartId(request.cartId(), cartIdHeader), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove cart item", description = "Removes one item from the cart.")
    CartResponse removeItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID itemId,
            @RequestParam(required = false) String cartId,
            @RequestHeader(name = "X-Cart-Id", required = false) String cartIdHeader) {
        return cartService.removeItem(user, resolveCartId(cartId, cartIdHeader), itemId);
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items from the cart.")
    CartResponse clearCart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String cartId,
            @RequestHeader(name = "X-Cart-Id", required = false) String cartIdHeader) {
        return cartService.clearCart(user, resolveCartId(cartId, cartIdHeader));
    }

    private String resolveCartId(String cartId, String cartIdHeader) {
        return cartId != null ? cartId : cartIdHeader;
    }
}
