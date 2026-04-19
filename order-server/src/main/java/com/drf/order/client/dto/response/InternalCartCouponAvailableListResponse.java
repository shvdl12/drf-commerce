package com.drf.order.client.dto.response;

import java.util.List;

public record InternalCartCouponAvailableListResponse(
        List<CartCouponResult> coupons
) {
}
