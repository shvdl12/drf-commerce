package com.drf.order.model.dto;

import lombok.Builder;

@Builder
public record AmountResult(
        int totalAmount,
        int productDiscountAmount,
        int productCouponDiscountAmount,
        int orderCouponDiscountAmount,
        int deliveryFee,
        int finalAmount) {
}
