package com.drf.coupon.discount;

public record DiscountContext(
        int orderAmount,
        Integer categoryAmount
) {
}
