package com.drf.order.facade;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.order.client.dto.response.DeliveryAddressResponse;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Cart;
import com.drf.order.entity.Order;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.CartItemsResult;
import com.drf.order.model.dto.OrderItemData;
import com.drf.order.model.dto.OrderLineItem;
import com.drf.order.model.request.OrderCreateRequest;
import com.drf.order.model.response.OrderCreateResponse;
import com.drf.order.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final CartService cartService;
    private final OrderService orderService;
    private final OrderPricingService orderPricingService;
    private final OrderProductService orderProductService;
    private final OrderCouponService orderCouponService;
    private final OrderMemberService orderMemberService;
    private final OrderPaymentService orderPaymentService;


    public OrderCreateResponse createOrder(AuthInfo authInfo, String idempotencyKey, OrderCreateRequest request) {
        long memberId = authInfo.id();

        // 장바구니 조회
        CartItemsResult cartResult = cartService.getValidatedCartItems(memberId, request.cartItemIds());
        Cart cart = cartResult.cart();

        // 상품 조회
        List<OrderLineItem> orderLineItems = orderProductService.getOrderLineItems(cartResult.items());

        // 쿠폰 할인 계산
        orderCouponService.calculateDiscounts(memberId, cart, orderLineItems);

        // 금액 계산 + 기대금액 검증
        AmountResult amounts = orderPricingService.calculateAmounts(orderLineItems);
        if (amounts.finalAmount() != request.expectedAmount()) {
            throw new BusinessException(ErrorCode.ORDER_AMOUNT_MISMATCH);
        }

        // 배송지 조회
        DeliveryAddressResponse address = orderMemberService.getDeliveryAddress(memberId, request.shippingAddressId());

        // 주문 생성 (PENDING)
        List<OrderItemData> orderItemData = orderLineItems.stream()
                .map(OrderLineItem::toOrderItemData)
                .toList();
        Order order = orderService.createOrder(memberId, orderItemData, address, cart.getCouponId(), amounts);

        // 재고 선점
        try {
            orderProductService.reserveStocks(orderLineItems, idempotencyKey);
        } catch (Exception e) {
            log.error("Stock batch reserve failed", e);
            orderService.failOrder(order.getId());
            throw new BusinessException(ErrorCode.ORDER_STOCK_INSUFFICIENT);
        }

        // 쿠폰 선점
        List<Long> allMemberCouponIds = orderCouponService.collectCouponIds(cart, orderLineItems);
        try {
            orderCouponService.reserveCoupons(allMemberCouponIds, memberId);
        } catch (Exception e) {
            log.error("Coupon batch reserve failed", e);
            orderProductService.releaseStocks(orderLineItems, idempotencyKey);
            orderService.failOrder(order.getId());
            throw new BusinessException(ErrorCode.ORDER_COUPON_UNAVAILABLE);
        }

        // 결제
        try {
            orderPaymentService.pay(order.getId(), amounts.finalAmount(), request.paymentMethodId());
        } catch (Exception e) {
            log.error("Payment failed for orderId={}", order.getId(), e);
            orderCouponService.releaseCoupons(allMemberCouponIds, memberId);
            orderProductService.releaseStocks(orderLineItems, idempotencyKey);
            orderService.failOrder(order.getId());
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_FAILED);
        }

        // 결제 완료 처리 (트랜잭션 커밋 후 Kafka ORDER_PAID 이벤트 자동 발행)
        orderService.completePayment(order.getId(), memberId, request.cartItemIds(), allMemberCouponIds);

        return new OrderCreateResponse(order.getId(), order.getOrderNo(), order.getStatus().name(), amounts.finalAmount());
    }
}
