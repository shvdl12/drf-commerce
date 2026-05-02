package com.drf.inventory.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductStockRedisRepository {
    private static final String REDIS_STOCK_KEY_PREFIX = "product:stock:";

    private static final DefaultRedisScript<Long> RESERVE_STOCK_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current == false then return -1 end
            if tonumber(current) < tonumber(ARGV[1]) then return -2 end
            return redis.call('DECRBY', KEYS[1], ARGV[1])
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_STOCK_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current == false then return -1 end
            return redis.call('INCRBY', KEYS[1], ARGV[1])
            """, Long.class);


    private final StringRedisTemplate redisTemplate;

    public Long getStock(long productId) {
        String stock = redisTemplate.opsForValue().get(generateKey(productId));
        return stock != null ? Long.parseLong(stock) : null;
    }

    public List<Long> getStocks(List<Long> productIds) {
        List<String> keys = productIds.stream()
                .map(this::generateKey)
                .toList();
        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) return List.of();

        return values.stream()
                .map(v -> v != null ? Long.parseLong(v) : null)
                .toList();
    }

    public void setStock(long productId, long stock) {
        redisTemplate.opsForValue().set(generateKey(productId), String.valueOf(stock));
    }

    public boolean deleteStock(long productId) {
        return redisTemplate.delete(generateKey(productId));
    }

    public int reserveStock(long productId, long quantity) {
        Long result = redisTemplate.execute(RESERVE_STOCK_SCRIPT, List.of(generateKey(productId)), String.valueOf(quantity));
        return result.intValue();
    }

    public int releaseStock(long productId, long quantity) {
        Long result = redisTemplate.execute(RELEASE_STOCK_SCRIPT, List.of(generateKey(productId)), String.valueOf(quantity));
        return result.intValue();
    }

    private String generateKey(long productId) {
        return REDIS_STOCK_KEY_PREFIX + productId;
    }
}

