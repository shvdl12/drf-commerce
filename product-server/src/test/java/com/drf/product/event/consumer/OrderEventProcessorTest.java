package com.drf.product.event.consumer;

import com.drf.product.repository.ProductStockRedisRepository;
import com.drf.product.repository.ProductStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OrderEventProcessorTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private ProductStockRedisRepository stockRedisRepository;

    @InjectMocks
    private OrderEventProcessor orderEventProcessor;

    @Test
    @DisplayName("processPaymentCompleted: ProcessedEvent를 저장하고 DB stock을 차감한다")
    void processPaymentCompleted_savesAndDecrements() {
        // given
        given(productStockRepository.decrementStock(10L, 5)).willReturn(1);

        // when
        orderEventProcessor.processPaymentCompleted(1L, 10L, 5);

        // then
        then(processedEventRepository).should().save(any(ProcessedEvent.class));
        then(productStockRepository).should().decrementStock(10L, 5);
    }

    @Test
    @DisplayName("processPaymentCompleted: 재고 부족 또는 상품 없으면 예외 없이 에러 로그를 출력한다")
    void processPaymentCompleted_failedDecrement_noException() {
        // given
        given(productStockRepository.decrementStock(999L, 5)).willReturn(0);

        // when & then
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> orderEventProcessor.processPaymentCompleted(1L, 999L, 5));
    }

    @Test
    @DisplayName("processRefundCompleted: ProcessedEvent를 저장하고 Redis + DB stock을 복원한다")
    void processRefundCompleted_savesAndRestores() {
        // given
        given(stockRedisRepository.releaseStock(10L, 5)).willReturn(100);
        given(productStockRepository.incrementStock(10L, 5)).willReturn(1);

        // when
        orderEventProcessor.processRefundCompleted(2L, 10L, 5);

        // then
        then(processedEventRepository).should().save(any(ProcessedEvent.class));
        then(stockRedisRepository).should().releaseStock(10L, 5);
        then(productStockRepository).should().incrementStock(10L, 5);
    }

    @Test
    @DisplayName("processRefundCompleted: ProductStock이 없으면 경고 로그만 출력하고 예외가 발생하지 않는다")
    void processRefundCompleted_productNotFound_noException() {
        // given
        given(stockRedisRepository.releaseStock(999L, 5)).willReturn(-1);
        given(productStockRepository.incrementStock(999L, 5)).willReturn(0);

        // when & then
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> orderEventProcessor.processRefundCompleted(2L, 999L, 5));
    }
}
