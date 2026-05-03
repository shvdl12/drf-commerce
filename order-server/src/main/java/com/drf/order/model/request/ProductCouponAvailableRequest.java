package com.drf.order.model.request;

import java.util.List;

public record ProductCouponAvailableRequest(
        long cartItemId,
        long productId,
        long price,
        int quantity,
        List<Long> categoryPath
) {
}
