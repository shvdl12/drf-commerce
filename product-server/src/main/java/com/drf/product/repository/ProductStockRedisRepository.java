package com.drf.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductStockRedisRepository {
    private static final String REDIS_STOCK_KEY_PREFIX = "product:stock:";
    private final StringRedisTemplate redisTemplate;

    public void setStock(long productId, int stock) {
        redisTemplate.opsForValue().set(generateKey(productId), String.valueOf(stock));
    }

    public boolean deleteStock(long productId) {
        return redisTemplate.delete(generateKey(productId));
    }

    private String generateKey(long productId) {
        return REDIS_STOCK_KEY_PREFIX + productId;
    }
}

