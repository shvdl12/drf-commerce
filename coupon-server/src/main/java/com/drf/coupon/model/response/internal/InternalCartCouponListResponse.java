package com.drf.coupon.model.response.internal;

import java.util.List;

public record InternalCartCouponListResponse(
        List<CartCouponResult> coupons
) {
}
