package com.luxora.commerce.cart.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.cart.dto.CartItemResponse;
import com.luxora.commerce.cart.dto.CartResponse;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CartService {

    private static final String ANONYMOUS_KEY_PREFIX = "anon:";
    private static final String USER_KEY_PREFIX = "user:";

    private final ProductVariantRepository variantRepository;
    private final CartStore cartStore;
    private final Duration cartTtl;

    public CartService(
            ProductVariantRepository variantRepository,
            CartStore cartStore,
            @Value("${luxora.cart.ttl:PT24H}") Duration cartTtl) {
        this.variantRepository = variantRepository;
        this.cartStore = cartStore;
        this.cartTtl = cartTtl;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(AuthenticatedUser user, String cartId) {
        CartIdentity identity = resolveCart(user, cartId);
        Map<UUID, Integer> quantities = cartStore.getQuantities(identity.storeKey());
        if (!quantities.isEmpty()) {
            cartStore.refreshTtl(identity.storeKey(), cartTtl);
        }
        return buildCart(identity.responseCartId(), quantities);
    }


    public Map<UUID, Integer> getAuthenticatedCartQuantities(UUID userId) {
        return cartStore.getQuantities(userCart(userId).storeKey());
    }

    public void clearAuthenticatedCart(UUID userId) {
        cartStore.clear(userCart(userId).storeKey());
    }
    @Transactional(readOnly = true)
    public CartResponse addItem(AuthenticatedUser user, String cartId, UUID variantId, int quantity) {
        validateQuantity(quantity);
        CartIdentity identity = resolveCart(user, cartId);
        Map<UUID, Integer> quantities = cartStore.getQuantities(identity.storeKey());
        int nextQuantity = quantities.getOrDefault(variantId, 0) + quantity;
        ProductVariant variant = getPurchasableVariant(variantId);
        validateStock(variant, nextQuantity);

        cartStore.putQuantity(identity.storeKey(), variantId, nextQuantity, cartTtl);
        quantities.put(variantId, nextQuantity);
        return buildCart(identity.responseCartId(), quantities);
    }

    @Transactional(readOnly = true)
    public CartResponse updateItem(AuthenticatedUser user, String cartId, UUID itemId, int quantity) {
        validateQuantity(quantity);
        CartIdentity identity = requireCart(user, cartId);
        Map<UUID, Integer> quantities = cartStore.getQuantities(identity.storeKey());
        if (!quantities.containsKey(itemId)) {
            throw new NotFoundException("CART_ITEM_NOT_FOUND", "Cart item not found");
        }

        ProductVariant variant = getPurchasableVariant(itemId);
        validateStock(variant, quantity);
        cartStore.putQuantity(identity.storeKey(), itemId, quantity, cartTtl);
        quantities.put(itemId, quantity);
        return buildCart(identity.responseCartId(), quantities);
    }

    @Transactional(readOnly = true)
    public CartResponse removeItem(AuthenticatedUser user, String cartId, UUID itemId) {
        CartIdentity identity = requireCart(user, cartId);
        cartStore.removeItem(identity.storeKey(), itemId);
        return getCart(user, identity.responseCartId());
    }

    public CartResponse clearCart(AuthenticatedUser user, String cartId) {
        CartIdentity identity = requireCart(user, cartId);
        cartStore.clear(identity.storeKey());
        return new CartResponse(identity.responseCartId(), List.of(), 0, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public CartResponse mergeAnonymousCartIntoUser(String anonymousCartId, UUID userId) {
        CartIdentity userCart = userCart(userId);
        if (!StringUtils.hasText(anonymousCartId)) {
            return getCart(new AuthenticatedUser(userId, "", List.of()), null);
        }

        CartIdentity anonymousCart = anonymousCart(anonymousCartId);
        Map<UUID, Integer> anonymousQuantities = cartStore.getQuantities(anonymousCart.storeKey());
        if (anonymousQuantities.isEmpty()) {
            return getCart(new AuthenticatedUser(userId, "", List.of()), null);
        }

        Map<UUID, Integer> mergedQuantities = new LinkedHashMap<>(cartStore.getQuantities(userCart.storeKey()));
        anonymousQuantities.forEach((variantId, quantity) ->
                mergedQuantities.merge(variantId, quantity, Integer::sum));

        validateMergedStock(mergedQuantities);
        mergedQuantities.forEach((variantId, quantity) ->
                cartStore.putQuantity(userCart.storeKey(), variantId, quantity, cartTtl));
        cartStore.clear(anonymousCart.storeKey());

        return buildCart(userCart.responseCartId(), mergedQuantities);
    }

    private void validateMergedStock(Map<UUID, Integer> quantities) {
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            ProductVariant variant = getPurchasableVariant(entry.getKey());
            if (variant.getInventoryItem().getQuantityAvailable() < entry.getValue()) {
                throw new BadRequestException(
                        "CART_MERGE_STOCK_LIMIT",
                        "Cart merge exceeds available stock for selected variant");
            }
        }
    }

    private CartResponse buildCart(String cartId, Map<UUID, Integer> quantities) {
        List<CartItemResponse> items = quantities.entrySet().stream()
                .map(entry -> toCartItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CartItemResponse::productName).thenComparing(CartItemResponse::sku))
                .toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotalTotal = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cartId, items, totalItems, subtotalTotal);
    }

    private CartItemResponse toCartItem(UUID variantId, int quantity) {
        ProductVariant variant = variantRepository.findWithProductById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Product variant not found"));
        BigDecimal subtotal = variant.getPrice().multiply(BigDecimal.valueOf(quantity));

        return new CartItemResponse(
                variant.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getProduct().getSlug(),
                variant.getSku(),
                variant.getColor(),
                variant.getStorage(),
                quantity,
                variant.getPrice(),
                subtotal,
                variant.getInventoryItem().isAvailable());
    }

    private ProductVariant getPurchasableVariant(UUID variantId) {
        ProductVariant variant = variantRepository.findWithProductById(variantId)
                .orElseThrow(() -> new NotFoundException("VARIANT_NOT_FOUND", "Product variant not found"));
        if (!variant.isActive() || !variant.getProduct().isActive()) {
            throw new BadRequestException("VARIANT_UNAVAILABLE", "Product variant is unavailable");
        }
        return variant;
    }

    private void validateStock(ProductVariant variant, int quantity) {
        if (variant.getInventoryItem().getQuantityAvailable() < quantity) {
            throw new BadRequestException("INSUFFICIENT_STOCK", "Not enough stock for selected variant");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("INVALID_QUANTITY", "Quantity must be greater than zero");
        }
    }

    private CartIdentity resolveCart(AuthenticatedUser user, String cartId) {
        if (user != null) {
            return userCart(user.id());
        }
        return anonymousCart(resolveAnonymousCartId(cartId));
    }

    private CartIdentity requireCart(AuthenticatedUser user, String cartId) {
        if (user != null) {
            return userCart(user.id());
        }
        if (!StringUtils.hasText(cartId)) {
            throw new BadRequestException("CART_ID_REQUIRED", "cartId is required");
        }
        return anonymousCart(cartId);
    }

    private CartIdentity userCart(UUID userId) {
        String userCartId = userId.toString();
        return new CartIdentity(USER_KEY_PREFIX + userCartId, userCartId);
    }

    private CartIdentity anonymousCart(String cartId) {
        String normalizedCartId = cartId.trim();
        return new CartIdentity(ANONYMOUS_KEY_PREFIX + normalizedCartId, normalizedCartId);
    }

    private String resolveAnonymousCartId(String cartId) {
        return StringUtils.hasText(cartId) ? cartId.trim() : UUID.randomUUID().toString();
    }

    private record CartIdentity(String storeKey, String responseCartId) {
    }
}

