package com.drf.order.client.dto.request;

import java.util.List;

public record InternalCartCouponRequest(
        long memberId,
        List<InternalCartCouponItemRequest> items
) {
}
