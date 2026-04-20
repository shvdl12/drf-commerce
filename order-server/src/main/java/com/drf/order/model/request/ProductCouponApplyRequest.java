package com.drf.order.model.request;

import java.util.List;

public record ProductCouponApplyRequest(
        long cartItemId,
        int price,
        int quantity,
        List<Long> categoryPath
) {
}
