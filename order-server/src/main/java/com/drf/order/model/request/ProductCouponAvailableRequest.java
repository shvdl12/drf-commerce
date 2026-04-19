package com.drf.order.model.request;

import java.util.List;

public record ProductCouponAvailableRequest(
        long cartItemId,
        long productId,
        int price,
        int quantity,
        List<Long> categoryPath
) {
}
