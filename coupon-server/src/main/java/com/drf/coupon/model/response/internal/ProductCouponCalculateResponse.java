package com.drf.coupon.model.response.internal;

public record ProductCouponCalculateResponse(
        boolean applicable,
        int discountAmount
) {
}
