package com.drf.coupon.model.request.internal;

import java.util.List;

public record InternalCartCouponItemRequest(
        long cartItemId,
        long productId,
        int price,
        int quantity,
        List<Long> categoryPath
) {
}
