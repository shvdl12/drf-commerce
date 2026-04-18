package com.drf.order.client.dto.response;

import java.util.List;

public record CartCouponResult(
        long memberCouponId,
        String name,
        int discountAmount,
        boolean isBest,
        List<InternalCouponItemResult> items
) {
}
