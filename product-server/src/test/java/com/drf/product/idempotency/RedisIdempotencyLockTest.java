package com.drf.product.idempotency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RedisIdempotencyLockTest {

    private static final String KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SCOPE = "STOCK_RESERVE";
    private static final String TOKEN = "test-token-uuid";
    private static final String EXPECTED_LOCK_KEY = "idempotency:lock:" + KEY + ":" + SCOPE;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @InjectMocks
    private RedisIdempotencyLock redisIdempotencyLock;

    @Test
    @DisplayName("락 선점 성공 - true 반환")
    void acquire_success() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(EXPECTED_LOCK_KEY), anyString(), eq(Duration.ofSeconds(30))))
                .willReturn(true);

        // when
        boolean result = redisIdempotencyLock.acquire(KEY, SCOPE, TOKEN);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("락 이미 존재 - false 반환")
    void acquire_alreadyLocked() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(EXPECTED_LOCK_KEY), anyString(), eq(Duration.ofSeconds(30))))
                .willReturn(false);

        // when
        boolean result = redisIdempotencyLock.acquire(KEY, SCOPE, TOKEN);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("락 획득 시 전달받은 token을 Redis value로 사용한다")
    void acquire_usesTokenAsValue() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.setIfAbsent(eq(EXPECTED_LOCK_KEY), eq(TOKEN), eq(Duration.ofSeconds(30))))
                .willReturn(true);

        // when
        redisIdempotencyLock.acquire(KEY, SCOPE, TOKEN);

        // then
        then(valueOperations).should().setIfAbsent(EXPECTED_LOCK_KEY, TOKEN, Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("락 해제 - 올바른 key와 token으로 Lua 스크립트를 실행한다")
    void release_executesLuaScriptWithKeyAndToken() {
        // when
        redisIdempotencyLock.release(KEY, SCOPE, TOKEN);

        // then
        then(redisTemplate).should().execute(
                any(RedisScript.class),
                eq(List.of(EXPECTED_LOCK_KEY)),
                eq(TOKEN)
        );
    }

    @Test
    @DisplayName("락 해제 - token 불일치 시에도 스크립트는 실행되며 Redis에서 0을 반환한다")
    void release_withDifferentToken_scriptReturnsZero() {
        // given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).willReturn(0L);

        // when
        redisIdempotencyLock.release(KEY, SCOPE, "wrong-token");

        // then: 스크립트는 실행되나 DEL은 Lua 내부에서 skip됨 (반환값 0)
        then(redisTemplate).should().execute(
                any(RedisScript.class),
                eq(List.of(EXPECTED_LOCK_KEY)),
                eq("wrong-token")
        );
    }
}
