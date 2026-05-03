package com.drf.order.service;

import com.drf.common.model.CommonResponse;
import com.drf.common.model.Money;
import com.drf.order.client.ProductClient;
import com.drf.order.client.dto.request.ProductBatchRequest;
import com.drf.order.client.dto.response.InternalProductResponse;
import com.drf.order.model.request.CheckoutItemRequest;
import com.drf.order.model.response.CheckoutResponse;
import com.drf.order.model.response.CheckoutUnavailableItem.UnavailableReason;
import com.drf.order.model.type.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @InjectMocks
    private CheckoutService checkoutService;

    @Mock
    private ProductClient productClient;

    @Mock
    private DeliveryFeePolicy deliveryFeePolicy;

    @Test
    @DisplayName("구매 가능한 상품은 availableItems에 포함되고 소계가 올바르게 계산된다")
    void checkout_availableItems() {
        InternalProductResponse product = product(1L, 10000L, 8000L, 10, ProductStatus.ON_SALE);
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of(product)));
        given(deliveryFeePolicy.calculateFee(any())).willReturn(Money.of(3000));

        CheckoutResponse response = checkoutService.checkout(List.of(new CheckoutItemRequest(1L, 2)));

        assertThat(response.availableItems()).hasSize(1);
        assertThat(response.availableItems().getFirst().subtotal()).isEqualTo(16000L); // 8000 * 2
        assertThat(response.unavailableItems()).isEmpty();
    }

    @Test
    @DisplayName("주문금액이 무료배송 기준 이상이면 배송비가 0이다")
    void checkout_freeShipping() {
        InternalProductResponse product = product(1L, 60000L, 60000L, 10, ProductStatus.ON_SALE);
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of(product)));
        given(deliveryFeePolicy.calculateFee(any())).willReturn(Money.ZERO);

        CheckoutResponse response = checkoutService.checkout(List.of(new CheckoutItemRequest(1L, 1)));

        assertThat(response.deliveryFee()).isZero();
        assertThat(response.totalAmount()).isEqualTo(response.orderAmount());
    }

    @Test
    @DisplayName("주문금액이 무료배송 기준 미만이면 배송비가 부과된다")
    void checkout_shippingFee() {
        InternalProductResponse product = product(1L, 10000L, 10000L, 10, ProductStatus.ON_SALE);
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of(product)));
        given(deliveryFeePolicy.calculateFee(any())).willReturn(Money.of(3000));

        CheckoutResponse response = checkoutService.checkout(List.of(new CheckoutItemRequest(1L, 1)));

        assertThat(response.deliveryFee()).isEqualTo(3000L);
        assertThat(response.totalAmount()).isEqualTo(response.orderAmount() + response.deliveryFee());
    }

    @Test
    @DisplayName("존재하지 않는 상품은 NOT_ON_SALE로 unavailableItems에 추가된다")
    void checkout_productNotFound() {
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of()));

        CheckoutResponse response = checkoutService.checkout(List.of(new CheckoutItemRequest(999L, 1)));

        assertThat(response.availableItems()).isEmpty();
        assertThat(response.unavailableItems()).hasSize(1);
        assertThat(response.unavailableItems().getFirst().reason()).isEqualTo(UnavailableReason.NOT_ON_SALE);
    }

    @Test
    @DisplayName("판매 중이 아닌 상품은 NOT_ON_SALE로 unavailableItems에 추가된다")
    void checkout_productNotOnSale() {
        InternalProductResponse product = product(1L, 10000L, 10000L, 10, ProductStatus.READY);
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of(product)));

        CheckoutResponse response = checkoutService.checkout(List.of(new CheckoutItemRequest(1L, 1)));

        assertThat(response.unavailableItems()).hasSize(1);
        assertThat(response.unavailableItems().getFirst().reason()).isEqualTo(UnavailableReason.NOT_ON_SALE);
    }

    @Test
    @DisplayName("재고가 0인 상품은 OUT_OF_STOCK으로 unavailableItems에 추가된다")
    void checkout_outOfStock() {
        InternalProductResponse product = product(1L, 10000L, 10000L, 0, ProductStatus.ON_SALE);
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of(product)));

        CheckoutResponse response = checkoutService.checkout(List.of(new CheckoutItemRequest(1L, 1)));

        assertThat(response.unavailableItems()).hasSize(1);
        assertThat(response.unavailableItems().getFirst().reason()).isEqualTo(UnavailableReason.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("재고가 요청 수량보다 적으면 INSUFFICIENT_STOCK으로 unavailableItems에 추가된다")
    void checkout_insufficientStock() {
        InternalProductResponse product = product(1L, 10000L, 10000L, 2, ProductStatus.ON_SALE);
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of(product)));

        CheckoutResponse response = checkoutService.checkout(List.of(new CheckoutItemRequest(1L, 5)));

        assertThat(response.unavailableItems()).hasSize(1);
        assertThat(response.unavailableItems().getFirst().reason()).isEqualTo(UnavailableReason.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("가용 상품과 불가 상품이 섞여 있으면 각각 올바른 목록에 분류된다")
    void checkout_mixedItems() {
        InternalProductResponse available = product(1L, 10000L, 10000L, 10, ProductStatus.ON_SALE);
        InternalProductResponse unavailable = product(2L, 5000L, 5000L, 0, ProductStatus.ON_SALE);
        given(productClient.getProductsBatch(any(ProductBatchRequest.class)))
                .willReturn(CommonResponse.success(List.of(available, unavailable)));
        given(deliveryFeePolicy.calculateFee(any())).willReturn(Money.of(3000));

        CheckoutResponse response = checkoutService.checkout(List.of(
                new CheckoutItemRequest(1L, 1),
                new CheckoutItemRequest(2L, 1)
        ));

        assertThat(response.availableItems()).hasSize(1);
        assertThat(response.availableItems().getFirst().productId()).isEqualTo(1L);
        assertThat(response.unavailableItems()).hasSize(1);
        assertThat(response.unavailableItems().getFirst().productId()).isEqualTo(2L);
    }

    private InternalProductResponse product(long id, long price, long discountedPrice, int stock, ProductStatus status) {
        return new InternalProductResponse(id, "상품" + id, "", price, 0, 0L, discountedPrice, 1L, List.of(), status, stock);
    }
}
