package com.drf.product.idempotency;

import com.drf.common.idempotency.IdempotencyLock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisIdempotencyLock implements IdempotencyLock {

    private static final String LOCK_KEY_PREFIX = "idempotency:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            else
                return 0
            end
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean acquire(String idempotencyKey, String scope, String token) {
        String lockKey = generateKey(idempotencyKey, scope);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, token, LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void release(String idempotencyKey, String scope, String token) {
        redisTemplate.execute(RELEASE_SCRIPT, List.of(generateKey(idempotencyKey, scope)), token);
    }

    private String generateKey(String idempotencyKey, String scope) {
        return LOCK_KEY_PREFIX + idempotencyKey + ":" + scope;
    }
}
