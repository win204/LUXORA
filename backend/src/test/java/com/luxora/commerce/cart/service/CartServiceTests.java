package com.luxora.commerce.cart.service;

import com.luxora.commerce.auth.security.AuthenticatedUser;
import com.luxora.commerce.catalog.model.Brand;
import com.luxora.commerce.catalog.model.Category;
import com.luxora.commerce.catalog.model.Product;
import com.luxora.commerce.catalog.model.ProductVariant;
import com.luxora.commerce.catalog.repository.ProductVariantRepository;
import com.luxora.commerce.common.exception.BadRequestException;
import com.luxora.commerce.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartServiceTests {

    private final ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
    private final InMemoryCartStore cartStore = new InMemoryCartStore();
    private final CartService cartService = new CartService(variantRepository, cartStore, Duration.ofHours(24));

    @Test
    void anonymousCartWorks() {
        ProductVariant variant = variant("sku-1", 10, "99.00");

        var cart = cartService.addItem(null, "cart-1", variant.getId(), 2);

        assertThat(cart.cartId()).isEqualTo("cart-1");
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(2);
        assertThat(cart.items().getFirst().unitPrice()).isEqualByComparingTo("99.00");
        assertThat(cart.subtotalTotal()).isEqualByComparingTo("198.00");
    }

    @Test
    void authenticatedCartWorks() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        AuthenticatedUser user = user();

        var cart = cartService.addItem(user, null, variant.getId(), 2);

        assertThat(cart.cartId()).isEqualTo(user.id().toString());
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(2);
    }

    @Test
    void createAnonymousCart() {
        var cart = cartService.getCart(null, null);

        assertThat(cart.cartId()).isNotBlank();
        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalItems()).isZero();
    }

    @Test
    void addSameVariantTwice() {
        ProductVariant variant = variant("sku-1", 10, "99.00");

        cartService.addItem(null, "cart-1", variant.getId(), 2);
        var cart = cartService.addItem(null, "cart-1", variant.getId(), 3);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(5);
    }

    @Test
    void updateQuantity() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        cartService.addItem(null, "cart-1", variant.getId(), 1);

        var cart = cartService.updateItem(null, "cart-1", variant.getId(), 4);

        assertThat(cart.items().getFirst().quantity()).isEqualTo(4);
    }

    @Test
    void invalidQuantity() {
        ProductVariant variant = variant("sku-1", 10, "99.00");

        assertThatThrownBy(() -> cartService.addItem(null, "cart-1", variant.getId(), 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity must be greater than zero");
    }

    @Test
    void insufficientStock() {
        ProductVariant variant = variant("sku-1", 1, "99.00");

        assertThatThrownBy(() -> cartService.addItem(null, "cart-1", variant.getId(), 2))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Not enough stock for selected variant");
    }

    @Test
    void removeItem() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        cartService.addItem(null, "cart-1", variant.getId(), 1);

        var cart = cartService.removeItem(null, "cart-1", variant.getId());

        assertThat(cart.items()).isEmpty();
    }

    @Test
    void clearCart() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        cartService.addItem(null, "cart-1", variant.getId(), 1);

        var cart = cartService.clearCart(null, "cart-1");

        assertThat(cart.items()).isEmpty();
        assertThat(cart.totalItems()).isZero();
    }

    @Test
    void unknownVariant() {
        UUID variantId = UUID.randomUUID();
        when(variantRepository.findWithProductById(variantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(null, "cart-1", variantId, 1))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product variant not found");
    }

    @Test
    void loginWithoutAnonymousCartKeepsUserCart() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        AuthenticatedUser user = user();
        cartService.addItem(user, null, variant.getId(), 2);

        var cart = cartService.mergeAnonymousCartIntoUser(null, user.id());

        assertThat(cart.cartId()).isEqualTo(user.id().toString());
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(2);
    }

    @Test
    void loginWithAnonymousCartMergesSameVariant() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        AuthenticatedUser user = user();
        cartService.addItem(user, null, variant.getId(), 2);
        cartService.addItem(null, "anon-1", variant.getId(), 3);

        var cart = cartService.mergeAnonymousCartIntoUser("anon-1", user.id());

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(5);
        assertThat(cartService.getCart(null, "anon-1").items()).isEmpty();
    }

    @Test
    void loginWithAnonymousCartMergesDifferentVariants() {
        ProductVariant first = variant("sku-1", 10, "99.00");
        ProductVariant second = variant("sku-2", 10, "149.00");
        AuthenticatedUser user = user();
        cartService.addItem(user, null, first.getId(), 1);
        cartService.addItem(null, "anon-1", second.getId(), 2);

        var cart = cartService.mergeAnonymousCartIntoUser("anon-1", user.id());

        assertThat(cart.items()).hasSize(2);
        assertThat(cart.totalItems()).isEqualTo(3);
    }

    @Test
    void insufficientStockDuringMergeRejectsCart() {
        ProductVariant variant = variant("sku-1", 4, "99.00");
        AuthenticatedUser user = user();
        cartService.addItem(user, null, variant.getId(), 3);
        cartService.addItem(null, "anon-1", variant.getId(), 2);

        assertThatThrownBy(() -> cartService.mergeAnonymousCartIntoUser("anon-1", user.id()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart merge exceeds available stock for selected variant");
    }

    @Test
    void userCannotAccessAnotherCart() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        AuthenticatedUser user = user();
        cartService.addItem(null, "someone-else", variant.getId(), 2);

        var cart = cartService.getCart(user, "someone-else");

        assertThat(cart.cartId()).isEqualTo(user.id().toString());
        assertThat(cart.items()).isEmpty();
    }

    @Test
    void logoutLoginPreservesUserCart() {
        ProductVariant variant = variant("sku-1", 10, "99.00");
        AuthenticatedUser user = user();
        cartService.addItem(user, null, variant.getId(), 2);

        var cart = cartService.getCart(user, null);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().quantity()).isEqualTo(2);
    }

    private ProductVariant variant(String sku, int stock, String price) {
        Brand brand = new Brand("Aurora Devices", "aurora-devices");
        Category category = new Category("Phones", "phones");
        Product product = new Product("AeroPhone X1", "aerophone-x1", "Phone", "Description", brand, category);
        ProductVariant variant = new ProductVariant(sku, "Graphite", "128GB", new BigDecimal(price), stock);
        product.addVariant(variant);

        UUID variantId = UUID.randomUUID();
        ReflectionTestUtils.setField(variant, "id", variantId);
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(variant.getInventoryItem(), "id", UUID.randomUUID());
        when(variantRepository.findWithProductById(variantId)).thenReturn(Optional.of(variant));
        return variant;
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(UUID.randomUUID(), "user@example.com", List.of("ROLE_USER"));
    }

    private static class InMemoryCartStore implements CartStore {

        private final Map<String, Map<UUID, Integer>> carts = new LinkedHashMap<>();

        @Override
        public Map<UUID, Integer> getQuantities(String cartId) {
            return new LinkedHashMap<>(carts.getOrDefault(cartId, Map.of()));
        }

        @Override
        public void putQuantity(String cartId, UUID variantId, int quantity, Duration ttl) {
            carts.computeIfAbsent(cartId, ignored -> new LinkedHashMap<>()).put(variantId, quantity);
        }

        @Override
        public void removeItem(String cartId, UUID variantId) {
            carts.getOrDefault(cartId, new LinkedHashMap<>()).remove(variantId);
        }

        @Override
        public void clear(String cartId) {
            carts.remove(cartId);
        }

        @Override
        public void refreshTtl(String cartId, Duration ttl) {
        }
    }
}
