package com.drf.order.service;

import com.drf.common.model.Money;
import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.OrderLineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final DeliveryFeePolicy deliveryFeePolicy;

    public AmountResult calculateAmounts(List<OrderLineItem> lineItems) {
        // 할인 전 주문 총액
        Money totalAmount = lineItems.stream()
                .map(OrderLineItem::grossAmount)
                .reduce(Money.ZERO, Money::add);

        // 자체 할인 총액
        Money productDiscountAmount = lineItems.stream()
                .map(OrderLineItem::getProductDiscountAmount)
                .reduce(Money.ZERO, Money::add);

        // 상품 쿠폰 할인 총액
        Money productCouponDiscountAmount = lineItems.stream()
                .map(OrderLineItem::getProductDiscountAmount)
                .reduce(Money.ZERO, Money::add);

        // 주문 쿠폰 할인 총액
        Money orderCouponDiscountAmount = lineItems.stream()
                .map(OrderLineItem::getOrderCouponDiscount)
                .reduce(Money.ZERO, Money::add);

        Money netAmount = totalAmount
                .subtract(productDiscountAmount)
                .subtract(productCouponDiscountAmount)
                .subtract(orderCouponDiscountAmount);

        Money deliveryFee = deliveryFeePolicy.calculateFee(netAmount);

        return AmountResult.builder()
                .totalAmount(totalAmount)
                .productDiscountAmount(productDiscountAmount)
                .productCouponDiscountAmount(productCouponDiscountAmount)
                .orderCouponDiscountAmount(orderCouponDiscountAmount)
                .deliveryFee(deliveryFee)
                .finalAmount(netAmount.add(deliveryFee))
                .build();
    }
}
