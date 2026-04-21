package com.drf.order.client.dto.request;

import java.util.List;

public record CouponBatchReserveRequest(List<CouponBatchReserveItem> items) {
    public record CouponBatchReserveItem(long memberCouponId, long memberId) {
    }
}
