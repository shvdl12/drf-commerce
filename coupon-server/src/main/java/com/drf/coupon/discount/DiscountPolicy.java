package com.drf.coupon.discount;

import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;

public interface DiscountPolicy {

    DiscountType getType();

    int calculate(Coupon coupon, int base);
}
