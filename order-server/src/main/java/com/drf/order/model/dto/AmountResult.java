package com.drf.order.model.dto;

import com.drf.common.model.Money;
import lombok.Builder;

@Builder
public record AmountResult(
        Money totalAmount,
        Money productDiscountAmount,
        Money productCouponDiscountAmount,
        Money orderCouponDiscountAmount,
        Money deliveryFee,
        Money finalAmount) {
}
