package com.drf.member.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistStore {

    private static final String KEY_PREFIX = "access_blacklist_token:";

    private final StringRedisTemplate redisTemplate;


    public void save(String accessToken, Duration remainingExpiry) {
        redisTemplate.opsForValue().set(KEY_PREFIX + accessToken, "blacklisted", remainingExpiry);
    }
}
