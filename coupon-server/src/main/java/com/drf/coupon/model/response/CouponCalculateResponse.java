package com.drf.coupon.model.response;

public record CouponCalculateResponse(
        int orderAmount,
        int discountAmount,
        int finalAmount
) {
}
