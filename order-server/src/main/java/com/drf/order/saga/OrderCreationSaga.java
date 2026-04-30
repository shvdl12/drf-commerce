package com.drf.order.saga;

import com.drf.common.exception.BusinessException;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.CartItemsResult;
import com.drf.order.model.dto.OrderItemData;
import com.drf.order.model.dto.OrderLineItem;
import com.drf.order.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private final OrderPaymentService orderPaymentService;

    public SagaDefinition<OrderSagaContext> definition() {
        // @formatter:off
        return SagaDefinition.<OrderSagaContext>builder()
                .step("loadCart")
                    .invokeLocal(this::loadCart)
                .step("loadProducts")
                    .invokeLocal(this::loadProducts)
                .step("calculateDiscounts")
                    .invokeLocal(this::calculateDiscounts)
                .step("validateAmount")
                    .invokeLocal(this::validateAmount)
                .step("loadAddress")
                    .invokeLocal(this::loadAddress)

                .step("createOrder")
                    .invokeLocal(this::createOrder)
                    .withCompensation(this::failOrder)
                .step("reserveStocks")
                    .invokeLocal(this::reserveStocks)
                    .withCompensation(this::releaseStocks)
                .step("reserveCoupons")
                    .invokeLocal(this::reserveCoupons)
                    .withCompensation(this::releaseCoupons)
                .step("pay")
                    .invokeLocal(this::pay)
                .step("completePayment")
                    .invokeLocal(this::completePayment)
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
        if (amounts.finalAmount() != ctx.getRequest().expectedAmount()) {
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
        ctx.setOrder(orderService.createOrder(ctx.getMemberId(), items, ctx.getAddress(),
                ctx.getCartItemsResult().cart().getCouponId(), ctx.getAmounts()));
        ctx.setReservedCouponIds(orderCouponService.collectCouponIds(ctx.getCartItemsResult().cart(), ctx.getLineItems()));
    }

    private void reserveStocks(OrderSagaContext ctx) {
        try {
            orderProductService.reserveStocks(ctx.getLineItems(), ctx.getIdempotencyKey());
        } catch (Exception e) {
            log.error("Stock reserve failed, orderId={}", ctx.getOrder().getId(), e);
            throw new BusinessException(ErrorCode.ORDER_STOCK_INSUFFICIENT);
        }
    }

    private void reserveCoupons(OrderSagaContext ctx) {
        try {
            orderCouponService.reserveCoupons(ctx.getReservedCouponIds(), ctx.getMemberId());
        } catch (Exception e) {
            log.error("Coupon reserve failed, orderId={}", ctx.getOrder().getId(), e);
            throw new BusinessException(ErrorCode.ORDER_COUPON_UNAVAILABLE);
        }
    }

    private void pay(OrderSagaContext ctx) {
        try {
            orderPaymentService.pay(ctx.getOrder().getId(), ctx.getAmounts().finalAmount(),
                    ctx.getRequest().paymentMethodId());
        } catch (Exception e) {
            log.error("Payment failed, orderId={}", ctx.getOrder().getId(), e);
            throw new BusinessException(ErrorCode.ORDER_PAYMENT_FAILED);
        }
    }

    private void completePayment(OrderSagaContext ctx) {
        orderService.completePayment(ctx.getOrder().getId(), ctx.getMemberId(),
                ctx.getRequest().cartItemIds(), ctx.getReservedCouponIds());
    }

    // --- 보상 메서드 ---

    private void failOrder(OrderSagaContext ctx) {
        orderService.failOrder(ctx.getOrder().getId());
    }

    private void releaseStocks(OrderSagaContext ctx) {
        orderProductService.releaseStocks(ctx.getLineItems(), ctx.getIdempotencyKey());
    }

    private void releaseCoupons(OrderSagaContext ctx) {
        orderCouponService.releaseCoupons(ctx.getReservedCouponIds(), ctx.getMemberId());
    }
}
