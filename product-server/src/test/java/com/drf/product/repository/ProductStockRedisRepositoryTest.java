package com.drf.product.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProductStockRedisRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ProductStockRedisRepository productStockRedisRepository;


    @Test
    @DisplayName("상품 재고를 Redis에 저장한다")
    void setStock() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        productStockRedisRepository.setStock(1L, 100);

        // then
        then(valueOperations).should().set("product:stock:1", "100");
    }

    @Test
    @DisplayName("재고가 충분하면 차감 후 남은 재고를 반환한다")
    void reserveStock_success() {
        // given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .willReturn(90L);

        // when
        int result = productStockRedisRepository.reserveStock(1L, 10);

        // then
        assertThat(result).isEqualTo(90);
    }

    @Test
    @DisplayName("Redis에 재고 키가 없으면 -1을 반환한다")
    void reserveStock_keyNotFound() {
        // given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .willReturn(-1L);

        // when
        int result = productStockRedisRepository.reserveStock(1L, 10);

        // then
        assertThat(result).isEqualTo(-1);
    }

    @Test
    @DisplayName("재고가 부족하면 -2를 반환한다")
    void reserveStock_insufficientStock() {
        // given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .willReturn(-2L);

        // when
        int result = productStockRedisRepository.reserveStock(1L, 9999);

        // then
        assertThat(result).isEqualTo(-2);
    }

    @Test
    @DisplayName("재고 해제 성공 - 복원 후 남은 재고를 반환한다")
    void releaseStock_success() {
        // given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .willReturn(100L);

        // when
        int result = productStockRedisRepository.releaseStock(1L, 10);

        // then
        assertThat(result).isEqualTo(100);
    }

    @Test
    @DisplayName("재고 해제 시 Redis에 키가 없으면 -1을 반환한다")
    void releaseStock_keyNotFound() {
        // given
        given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .willReturn(-1L);

        // when
        int result = productStockRedisRepository.releaseStock(1L, 10);

        // then
        assertThat(result).isEqualTo(-1);
    }
}
