package com.drf.member.infrastructure.redis;

import com.drf.member.common.auth.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh_token:";

    private final StringRedisTemplate redisTemplate;
    private final Duration refreshTokenExpiry;

    public RefreshTokenStore(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-expiry}") int refreshExpiry) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiry = Duration.ofSeconds(refreshExpiry);
    }

    public void save(Long id, Role role, String refreshToken) {
        redisTemplate.opsForValue().set(key(id, role), refreshToken, refreshTokenExpiry);
    }

    public String get(Long id, Role role) {
        return redisTemplate.opsForValue().get(key(id, role));
    }

    public void delete(Long id, Role role) {
        redisTemplate.delete(key(id, role));
    }

    private String key(Long id, Role role) {
        return KEY_PREFIX + role.name().toLowerCase() + ":" + id;
    }
}
