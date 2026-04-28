package com.drf.coupon.discount;

import com.drf.common.money.Money;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;

public interface DiscountStrategy {

    DiscountType getType();

    Money calculate(Coupon coupon, Money applicableAmount);
}
