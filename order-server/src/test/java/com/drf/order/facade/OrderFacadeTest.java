package com.drf.order.facade;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.order.client.dto.response.DeliveryAddressResponse;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Cart;
import com.drf.order.entity.CartItem;
import com.drf.order.entity.Order;
import com.drf.order.entity.OrderStatus;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.CartItemsResult;
import com.drf.order.model.dto.OrderLineItem;
import com.drf.order.model.request.OrderCreateRequest;
import com.drf.order.model.response.OrderCreateResponse;
import com.drf.order.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderFacadeTest {

    private static final long MEMBER_ID = 1L;
    private static final long CART_ITEM_ID = 10L;
    private static final long PRODUCT_ID = 100L;
    private static final long ORDER_ID = 1L;
    private static final long ADDRESS_ID = 100L;
    private static final long MEMBER_COUPON_ID = 200L;
    private static final String IDEMPOTENCY_KEY = "test-idem-key";

    // unitPrice=10000, unitDiscount=1000, discountedUnitPrice=9000, quantity=2
    // 쿠폰 없음: netAmount=18000(20000-2000), finalAmount=21000(+3000 배송)
    // 상품쿠폰 1000: netAmount=17000(20000-2000-1000), finalAmount=20000(+3000 배송)
    private static final int EXPECTED_AMOUNT_NO_COUPON = 21_000;
    private static final int EXPECTED_AMOUNT_WITH_PRODUCT_COUPON = 20_000;

    @Mock
    private CartService cartService;
    @Mock
    private OrderService orderService;
    @Mock
    private OrderPricingService orderPricingService;
    @Mock
    private OrderProductService orderProductService;
    @Mock
    private OrderCouponService orderCouponService;
    @Mock
    private OrderMemberService orderMemberService;
    @Mock
    private OrderPaymentService orderPaymentService;

    @InjectMocks
    private OrderFacade orderFacade;

    private Order order;
    private AmountResult noCouponAmounts;
    private AmountResult productCouponAmounts;

    @BeforeEach
    void setUp() {
        Cart cart = Cart.of(MEMBER_ID);

        CartItem cartItem = CartItem.of(1L, PRODUCT_ID, 2);
        ReflectionTestUtils.setField(cartItem, "id", CART_ITEM_ID);

        OrderLineItem lineItem = OrderLineItem.builder()
                .cartItemId(CART_ITEM_ID)
                .productId(PRODUCT_ID)
                .productName("테스트 상품")
                .unitPrice(10_000)
                .unitDiscountAmount(1_000)
                .discountedUnitPrice(9_000)
                .quantity(2)
                .categoryPath(List.of(1L, 2L))
                .build();

        DeliveryAddressResponse address = new DeliveryAddressResponse(
                "홍길동", "010-1234-5678", "12345", "서울시 강남구", "101호");

        order = Order.builder()
                .id(ORDER_ID)
                .orderNo("ORD-TEST-001")
                .status(OrderStatus.PAID)
                .build();

        noCouponAmounts = AmountResult.builder()
                .totalAmount(20_000)
                .productDiscountAmount(2_000)
                .productCouponDiscountAmount(0)
                .orderCouponDiscountAmount(0)
                .deliveryFee(3_000)
                .finalAmount(EXPECTED_AMOUNT_NO_COUPON)
                .build();

        productCouponAmounts = AmountResult.builder()
                .totalAmount(20_000)
                .productDiscountAmount(2_000)
                .productCouponDiscountAmount(1_000)
                .orderCouponDiscountAmount(0)
                .deliveryFee(3_000)
                .finalAmount(EXPECTED_AMOUNT_WITH_PRODUCT_COUPON)
                .build();

        given(cartService.getValidatedCartItems(MEMBER_ID, List.of(CART_ITEM_ID)))
                .willReturn(new CartItemsResult(cart, List.of(cartItem)));
        given(orderProductService.getOrderLineItems(anyList()))
                .willReturn(List.of(lineItem));
        given(orderCouponService.collectCouponIds(any(), anyList()))
                .willReturn(List.of());
        given(orderPricingService.calculateAmounts(anyList()))
                .willReturn(noCouponAmounts);
        given(orderMemberService.getDeliveryAddress(MEMBER_ID, ADDRESS_ID))
                .willReturn(address);
        given(orderService.createOrder(anyLong(), anyList(), any(), any(), any()))
                .willReturn(order);
    }

    @Test
    @DisplayName("쿠폰 없이 주문 생성 성공")
    void createOrder_success_noCoupon() {
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(CART_ITEM_ID), ADDRESS_ID, "CARD", EXPECTED_AMOUNT_NO_COUPON);

        OrderCreateResponse response = orderFacade.createOrder(new AuthInfo(MEMBER_ID), IDEMPOTENCY_KEY, request);

        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.finalAmount()).isEqualTo(EXPECTED_AMOUNT_NO_COUPON);

        then(orderService).should().completePayment(eq(ORDER_ID), eq(MEMBER_ID), anyList(), anyList());
        then(orderService).should(never()).failOrder(anyLong());
    }

    @Test
    @DisplayName("기대 금액 불일치 시 예외 발생 — 주문 생성 전에 실패")
    void createOrder_amountMismatch_throwsException() {
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(CART_ITEM_ID), ADDRESS_ID, "CARD", 99_999);

        assertThatThrownBy(() -> orderFacade.createOrder(new AuthInfo(MEMBER_ID), IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_AMOUNT_MISMATCH));

        then(orderService).should(never()).createOrder(anyLong(), anyList(), any(), any(), any());
        then(orderService).should(never()).failOrder(anyLong());
    }

    @Test
    @DisplayName("재고 선점 실패 시 주문 실패 처리 — 재고/쿠폰 해제 없음")
    void createOrder_stockReserveFail_failsOrderOnly() {
        OrderCreateRequest request = new OrderCreateRequest(
                List.of(CART_ITEM_ID), ADDRESS_ID, "CARD", EXPECTED_AMOUNT_NO_COUPON);

        willThrow(new RuntimeException("재고 부족"))
                .given(orderProductService).reserveStocks(anyList(), anyString());

        assertThatThrownBy(() -> orderFacade.createOrder(new AuthInfo(MEMBER_ID), IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_STOCK_INSUFFICIENT));

        then(orderService).should().failOrder(ORDER_ID);
        then(orderProductService).should(never()).releaseStocks(anyList(), anyString());
        then(orderCouponService).should(never()).releaseCoupons(anyList(), anyLong());
    }

    @Test
    @DisplayName("쿠폰 선점 실패 시 재고 해제 후 주문 실패 처리 — 쿠폰 해제 없음")
    void createOrder_couponReserveFail_releasesStockAndFailsOrder() {
        given(orderPricingService.calculateAmounts(anyList())).willReturn(productCouponAmounts);
        given(orderCouponService.collectCouponIds(any(), anyList())).willReturn(List.of(MEMBER_COUPON_ID));
        willThrow(new RuntimeException("쿠폰 선점 실패"))
                .given(orderCouponService).reserveCoupons(anyList(), anyLong());

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(CART_ITEM_ID), ADDRESS_ID, "CARD", EXPECTED_AMOUNT_WITH_PRODUCT_COUPON);

        assertThatThrownBy(() -> orderFacade.createOrder(new AuthInfo(MEMBER_ID), IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_COUPON_UNAVAILABLE));

        then(orderProductService).should().releaseStocks(anyList(), anyString());
        then(orderService).should().failOrder(ORDER_ID);
        then(orderCouponService).should(never()).releaseCoupons(anyList(), anyLong());
    }

    @Test
    @DisplayName("결제 실패 시 쿠폰/재고 해제 후 주문 실패 처리")
    void createOrder_paymentFail_releasesCouponAndStockAndFailsOrder() {
        given(orderPricingService.calculateAmounts(anyList())).willReturn(productCouponAmounts);
        given(orderCouponService.collectCouponIds(any(), anyList())).willReturn(List.of(MEMBER_COUPON_ID));
        willThrow(new RuntimeException("결제 실패"))
                .given(orderPaymentService).pay(anyLong(), anyInt(), anyString());

        OrderCreateRequest request = new OrderCreateRequest(
                List.of(CART_ITEM_ID), ADDRESS_ID, "CARD", EXPECTED_AMOUNT_WITH_PRODUCT_COUPON);

        assertThatThrownBy(() -> orderFacade.createOrder(new AuthInfo(MEMBER_ID), IDEMPOTENCY_KEY, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_PAYMENT_FAILED));

        then(orderCouponService).should().releaseCoupons(anyList(), anyLong());
        then(orderProductService).should().releaseStocks(anyList(), anyString());
        then(orderService).should().failOrder(ORDER_ID);
    }
}
