package com.drf.coupon.model.response.internal;

import lombok.Getter;

import java.util.List;

@Getter
public class CartCouponResult {

    private final long memberCouponId;
    private final String name;
    private final int discountAmount;
    private boolean isBest;
    private final List<InternalCouponItemResult> items;

    public CartCouponResult(long memberCouponId, String name, int discountAmount, List<InternalCouponItemResult> items) {
        this.memberCouponId = memberCouponId;
        this.name = name;
        this.discountAmount = discountAmount;
        this.isBest = false;
        this.items = items;
    }

    public void markAsBest() {
        this.isBest = true;
    }
}
