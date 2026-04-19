package com.drf.order.client.dto.response;

public record ProductCouponCalculateResponse(
        boolean applicable,
        int discountAmount
) {
}
