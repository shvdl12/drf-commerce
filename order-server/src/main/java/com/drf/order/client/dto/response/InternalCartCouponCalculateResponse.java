package com.drf.order.client.dto.response;

import java.util.List;

public record InternalCartCouponCalculateResponse(
        boolean applicable,
        int totalDiscountAmount,
        List<InternalCouponItemResult> items
) {
}
