package com.drf.coupon.model.response.internal;

public record InternalCouponItemResult(
        long cartItemId,
        long productId,
        boolean appliedYn,
        int discountAmount
) {
}
