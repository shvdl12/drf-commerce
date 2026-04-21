package com.drf.product.service;

import com.drf.common.exception.BusinessException;
import com.drf.product.common.exception.ErrorCode;
import com.drf.product.model.request.StockBatchReleaseRequest;
import com.drf.product.model.request.StockBatchReserveRequest;
import com.drf.product.repository.ProductRepository;
import com.drf.product.repository.ProductStockRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @InjectMocks
    private StockService stockService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductStockRedisRepository stockRedisRepository;

    @Nested
    @DisplayName("재고 선점")
    class ReserveProductStock {

        @Test
        @DisplayName("재고 선점 성공 - 남은 재고를 반환한다")
        void success() {
            // given
            long productId = 1L;
            var item = new StockBatchReserveRequest.StockBatchReserveItem(productId, 10);
            StockBatchReserveRequest request = new StockBatchReserveRequest(List.of(item));

            given(productRepository.existsById(productId)).willReturn(true);
            given(stockRedisRepository.reserveStock(productId, 10)).willReturn(90);

            // when
            stockService.batchReserveStock(request);
        }

        @Test
        @DisplayName("DB에 상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외 발생")
        void fail_productNotFoundInDb() {
            // given
            long productId = 999L;
            var item = new StockBatchReserveRequest.StockBatchReserveItem(productId, 10);
            StockBatchReserveRequest request = new StockBatchReserveRequest(List.of(item));

            given(productRepository.existsById(productId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> stockService.batchReserveStock(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("Redis에 재고 키가 없으면 PRODUCT_NOT_FOUND 예외 발생")
        void fail_stockKeyNotFoundInRedis() {
            // given
            long productId = 1L;
            var item = new StockBatchReserveRequest.StockBatchReserveItem(productId, 10);
            StockBatchReserveRequest request = new StockBatchReserveRequest(List.of(item));

            given(productRepository.existsById(productId)).willReturn(true);
            given(stockRedisRepository.reserveStock(productId, 10)).willReturn(-1);

            // when & then
            assertThatThrownBy(() -> stockService.batchReserveStock(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("재고가 부족하면 INSUFFICIENT_STOCK 예외 발생")
        void fail_insufficientStock() {
            // given
            long productId = 1L;
            var item = new StockBatchReserveRequest.StockBatchReserveItem(productId, 10);
            StockBatchReserveRequest request = new StockBatchReserveRequest(List.of(item));

            given(productRepository.existsById(productId)).willReturn(true);
            given(stockRedisRepository.reserveStock(productId, 10)).willReturn(-2);

            // when & then
            assertThatThrownBy(() -> stockService.batchReserveStock(request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    @Nested
    @DisplayName("재고 선점 해제")
    class ReleaseProductStock {

        @Test
        @DisplayName("재고 해제 성공 - 복원된 재고를 반환한다")
        void success() {
            // given
            long productId = 1L;
            var item = new StockBatchReleaseRequest.StockBatchReleaseItem(productId, 10);
            StockBatchReleaseRequest request = new StockBatchReleaseRequest(List.of(item));

            given(productRepository.existsById(productId)).willReturn(true);
            given(stockRedisRepository.releaseStock(productId, 10)).willReturn(100);

            // when
            stockService.batchReleaseStock(request);
        }

        @Test
        @DisplayName("DB에 상품이 존재하지 않아도 best-effort라 예외가 전파되지 않는다")
        void fail_productNotFoundInDb() {
            // given
            long productId = 999L;
            var item = new StockBatchReleaseRequest.StockBatchReleaseItem(productId, 10);
            StockBatchReleaseRequest request = new StockBatchReleaseRequest(List.of(item));

            given(productRepository.existsById(productId)).willReturn(false);

            // when & then
            assertThatCode(() -> stockService.batchReleaseStock(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Redis에 재고 키가 없어도 best-effort라 예외가 전파되지 않는다")
        void fail_stockKeyNotFoundInRedis() {
            // given
            long productId = 1L;
            var item = new StockBatchReleaseRequest.StockBatchReleaseItem(productId, 10);
            StockBatchReleaseRequest request = new StockBatchReleaseRequest(List.of(item));

            given(productRepository.existsById(productId)).willReturn(true);
            given(stockRedisRepository.releaseStock(productId, 10)).willReturn(-1);

            // when & then
            assertThatCode(() -> stockService.batchReleaseStock(request))
                    .doesNotThrowAnyException();
        }
    }
}
