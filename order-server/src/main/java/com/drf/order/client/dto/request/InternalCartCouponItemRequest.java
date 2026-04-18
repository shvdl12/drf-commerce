package com.drf.order.client.dto.request;

import java.util.List;

public record InternalCartCouponItemRequest(
        long cartItemId,
        long productId,
        int price,
        int quantity,
        List<Long> categoryPath
) {
}
