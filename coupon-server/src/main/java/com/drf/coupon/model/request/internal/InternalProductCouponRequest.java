package com.drf.coupon.model.request.internal;

import java.util.List;

public record InternalProductCouponRequest(
        long memberId,
        long cartItemId,
        long productId,
        int price,
        int quantity,
        List<Long> categoryPath,
        List<Long> usedMemberCouponIds
) {
}
