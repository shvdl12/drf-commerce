package com.drf.order.saga;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.Money;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.entity.Order;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.CartItemsResult;
import com.drf.order.model.dto.OrderItemData;
import com.drf.order.model.dto.OrderLineItem;
import com.drf.order.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreationSaga {

    private final CartService cartService;
    private final OrderService orderService;
    private final OrderPricingService orderPricingService;
    private final OrderProductService orderProductService;
    private final OrderCouponService orderCouponService;
    private final OrderMemberService orderMemberService;


    public SagaDefinition<OrderSagaContext> definition() {
        // @formatter:off
        return SagaDefinition.<OrderSagaContext>builder()
                .name("OrderCreationSaga")
                .step("loadCart")
                    .invoke(this::loadCart)
                .step("loadProducts")
                    .invoke(this::loadProducts)
                .step("calculateDiscounts")
                    .invoke(this::calculateDiscounts)
                .step("validateAmount")
                    .invoke(this::validateAmount)
                .step("loadAddress")
                    .invoke(this::loadAddress)

                .step("createOrder")
                    .invoke(this::createOrder)
                    .withCompensation(this::failOrder)
                .step("reserveStocks")
                    .invoke(this::reserveStocks)
                    .withCompensation(this::releaseStocks)
                .step("reserveCoupons")
                    .invoke(this::reserveCoupons)
                    .withCompensation(this::releaseCoupons)
                .build();
        // @formatter:on
    }

    // --- Step 메서드 ---

    private void loadCart(OrderSagaContext ctx) {
        CartItemsResult result = cartService.getValidatedCartItems(ctx.getMemberId(), ctx.getRequest().cartItemIds());
        ctx.setCartItemsResult(result);
    }

    private void loadProducts(OrderSagaContext ctx) {
        List<OrderLineItem> lineItems = orderProductService.getOrderLineItems(ctx.getCartItemsResult().items());
        ctx.setLineItems(lineItems);
    }

    private void calculateDiscounts(OrderSagaContext ctx) {
        orderCouponService.calculateDiscounts(ctx.getMemberId(), ctx.getCartItemsResult().cart(), ctx.getLineItems());
    }

    private void validateAmount(OrderSagaContext ctx) {
        AmountResult amounts = orderPricingService.calculateAmounts(ctx.getLineItems());
        if (!Objects.equals(amounts.finalAmount(), Money.of(ctx.getRequest().expectedAmount()))) {
            throw new BusinessException(ErrorCode.ORDER_AMOUNT_MISMATCH);
        }
        ctx.setAmounts(amounts);
    }

    private void loadAddress(OrderSagaContext ctx) {
        ctx.setAddress(orderMemberService.getDeliveryAddress(ctx.getMemberId(), ctx.getRequest().shippingAddressId()));
    }

    private void createOrder(OrderSagaContext ctx) {
        List<OrderItemData> items = ctx.getLineItems().stream()
                .map(com.drf.order.model.dto.OrderLineItem::toOrderItemData)
                .toList();
        Order order = orderService.createOrder(ctx.getMemberId(), items, ctx.getAddress(),
                ctx.getCartItemsResult().cart().getCouponId(), ctx.getAmounts());
        ctx.setOrderId(order.getId());
        ctx.setOrderNo(order.getOrderNo());
        ctx.setOrderStatus(order.getStatus());
        ctx.setReservedCouponIds(orderCouponService.collectCouponIds(ctx.getCartItemsResult().cart(), ctx.getLineItems()));
    }

    private void reserveStocks(OrderSagaContext ctx) {
        try {
            orderProductService.reserveStocks(ctx.getLineItems(), ctx.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Stock reserve failed, orderId={}", ctx.getOrderId(), e);
            throw new BusinessException(ErrorCode.ORDER_STOCK_INSUFFICIENT);
        }
    }

    private void reserveCoupons(OrderSagaContext ctx) {
        try {
            orderCouponService.reserveCoupons(ctx.getReservedCouponIds(), ctx.getMemberId());
        } catch (Exception e) {
            log.error("Coupon reserve failed, orderId={}", ctx.getOrderId(), e);
            throw new BusinessException(ErrorCode.ORDER_COUPON_UNAVAILABLE);
        }
    }

    // --- 보상 메서드 ---

    private void failOrder(OrderSagaContext ctx) {
        orderService.failOrder(ctx.getOrderId());
    }

    private void releaseStocks(OrderSagaContext ctx) {
        orderProductService.releaseStocks(ctx.getLineItems(), ctx.getIdempotencyKey());
    }

    private void releaseCoupons(OrderSagaContext ctx) {
        orderCouponService.releaseCoupons(ctx.getReservedCouponIds(), ctx.getMemberId());
    }
}
