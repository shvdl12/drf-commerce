package com.drf.order.model.dto;

import lombok.Builder;

@Builder
public record OrderItemData(
        long productId,
        String productName,
        int unitPrice,
        int discountedPrice,
        int quantity,
        int productCouponDiscountAmount,
        int orderCouponDiscountAmount,
        int finalAmount,
        Long memberCouponId
) {
}
