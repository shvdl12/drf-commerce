package com.drf.order.client.dto.response;

public record InternalCouponItemResult(
        long cartItemId,
        boolean appliedYn,
        int discountAmount
) {
}
