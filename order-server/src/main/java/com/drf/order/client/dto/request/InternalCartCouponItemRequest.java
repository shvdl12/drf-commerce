package com.drf.order.client.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record InternalCartCouponItemRequest(
        long cartItemId,
        long productId,
        int lineAmount,
        int quantity,
        List<Long> categoryPath
) {
}
