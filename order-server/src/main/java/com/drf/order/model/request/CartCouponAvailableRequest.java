package com.drf.order.model.request;

import java.util.List;

public record CartCouponAvailableRequest(
        long cartItemId,
        long productId,
        int price,
        int quantity,
        List<Long> categoryPath
) {
}
