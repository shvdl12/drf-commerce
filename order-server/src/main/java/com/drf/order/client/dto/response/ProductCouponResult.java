package com.drf.order.client.dto.response;

public record ProductCouponResult(
        long memberCouponId,
        String name,
        int discountAmount,
        boolean isBest,
        boolean usedOnOtherItem
) {
}
