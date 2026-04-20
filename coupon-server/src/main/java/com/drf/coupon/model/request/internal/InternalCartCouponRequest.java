package com.drf.coupon.model.request.internal;

import java.util.List;

public record InternalCartCouponRequest(
        long memberId,
        List<InternalCartCouponItemRequest> items
) {
}
