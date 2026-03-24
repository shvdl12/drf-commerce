package com.drf.product.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

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

    public int reserveStock(long productId, int quantity) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(
                        "local current = redis.call('GET', KEYS[1]) " +
                                "if current == false then return -1 end " +
                                "if tonumber(current) < tonumber(ARGV[1]) then return -2 end " +
                                "return redis.call('DECRBY', KEYS[1], ARGV[1])",
                        Long.class
                ),
                List.of(generateKey(productId)),
                String.valueOf(quantity)
        );
        return result.intValue();
    }

    public int releaseStock(long productId, int quantity) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(
                        "local current = redis.call('GET', KEYS[1]) " +
                                "if current == false then return -1 end " +
                                "return redis.call('INCRBY', KEYS[1], ARGV[1])",
                        Long.class
                ),
                List.of(generateKey(productId)),
                String.valueOf(quantity)
        );
        return result.intValue();
    }

    private String generateKey(long productId) {
        return REDIS_STOCK_KEY_PREFIX + productId;
    }
}

