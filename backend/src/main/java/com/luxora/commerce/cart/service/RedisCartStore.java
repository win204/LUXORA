package com.luxora.commerce.cart.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
class RedisCartStore implements CartStore {

    private static final String KEY_PREFIX = "cart:";

    private final StringRedisTemplate redisTemplate;

    RedisCartStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Map<UUID, Integer> getQuantities(String cartId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(cartId));
        Map<UUID, Integer> quantities = new LinkedHashMap<>();

        values.forEach((variantId, quantity) ->
                quantities.put(UUID.fromString(variantId.toString()), Integer.parseInt(quantity.toString())));

        return quantities;
    }

    @Override
    public void putQuantity(String cartId, UUID variantId, int quantity, Duration ttl) {
        redisTemplate.opsForHash().put(key(cartId), variantId.toString(), Integer.toString(quantity));
        refreshTtl(cartId, ttl);
    }

    @Override
    public void removeItem(String cartId, UUID variantId) {
        redisTemplate.opsForHash().delete(key(cartId), variantId.toString());
    }

    @Override
    public void clear(String cartId) {
        redisTemplate.delete(key(cartId));
    }

    @Override
    public void refreshTtl(String cartId, Duration ttl) {
        redisTemplate.expire(key(cartId), ttl);
    }

    private String key(String cartId) {
        return KEY_PREFIX + cartId;
    }
}
