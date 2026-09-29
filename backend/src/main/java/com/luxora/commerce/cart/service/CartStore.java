package com.luxora.commerce.cart.service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public interface CartStore {

    Map<UUID, Integer> getQuantities(String cartId);

    void putQuantity(String cartId, UUID variantId, int quantity, Duration ttl);

    void removeItem(String cartId, UUID variantId);

    void clear(String cartId);

    void refreshTtl(String cartId, Duration ttl);
}
