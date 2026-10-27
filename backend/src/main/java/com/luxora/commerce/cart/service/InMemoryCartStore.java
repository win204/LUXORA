package com.luxora.commerce.cart.service;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "luxora.cart.store", havingValue = "in-memory")
class InMemoryCartStore implements CartStore {

    private final Map<String, Map<UUID, Integer>> store = new ConcurrentHashMap<>();

    @Override
    public Map<UUID, Integer> getQuantities(String cartId) {
        Map<UUID, Integer> items = store.get(cartId);
        if (items == null) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<>(items);
    }

    @Override
    public void putQuantity(String cartId, UUID variantId, int quantity, Duration ttl) {
        store.computeIfAbsent(cartId, k -> new ConcurrentHashMap<>()).put(variantId, quantity);
    }

    @Override
    public void removeItem(String cartId, UUID variantId) {
        Map<UUID, Integer> items = store.get(cartId);
        if (items != null) {
            items.remove(variantId);
        }
    }

    @Override
    public void clear(String cartId) {
        store.remove(cartId);
    }

    @Override
    public void refreshTtl(String cartId, Duration ttl) {
        // No-op for in-memory store in demo mode
    }
}
