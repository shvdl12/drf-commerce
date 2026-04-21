package com.drf.coupon.model.request.internal;

import java.util.List;

public record InternalCouponBatchReserveRequest(
        List<InternalCouponBatchReserveItem> items
) {
    public record InternalCouponBatchReserveItem(long memberCouponId, long memberId) {
    }
}
