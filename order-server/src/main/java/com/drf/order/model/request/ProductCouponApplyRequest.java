package com.drf.order.model.request;

import java.util.List;

public record ProductCouponApplyRequest(
        long cartItemId,
        long price,
        int quantity,
        List<Long> categoryPath
) {
}
