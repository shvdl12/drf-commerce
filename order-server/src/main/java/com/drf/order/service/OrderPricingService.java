package com.drf.order.service;

import com.drf.order.model.dto.AmountResult;
import com.drf.order.model.dto.OrderLineItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderPricingService {
    private static final int FREE_SHIPPING_THRESHOLD = 50_000;
    private static final int SHIPPING_FEE = 3_000;

    public AmountResult calculateAmounts(List<OrderLineItem> lineItems) {
        int totalAmount = lineItems.stream().mapToInt(OrderLineItem::grossAmount).sum();

        // 자체 할인 총액
        int productDiscountAmount = lineItems.stream().mapToInt(OrderLineItem::getProductDiscountAmount).sum();

        // 상품 쿠폰 할인 총액
        int productCouponDiscountAmount = lineItems.stream().mapToInt(OrderLineItem::getProductCouponDiscount).sum();

        // 주문 쿠폰 할인 총액
        int orderCouponDiscountAmount = lineItems.stream().mapToInt(OrderLineItem::getOrderCouponDiscount).sum();

        int netAmount = totalAmount - productDiscountAmount - productCouponDiscountAmount - orderCouponDiscountAmount;
        int deliveryFee = netAmount >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;

        return AmountResult.builder()
                .totalAmount(totalAmount)
                .productDiscountAmount(productDiscountAmount)
                .productCouponDiscountAmount(productCouponDiscountAmount)
                .orderCouponDiscountAmount(orderCouponDiscountAmount)
                .deliveryFee(deliveryFee)
                .finalAmount(netAmount + deliveryFee)
                .build();
    }
}
