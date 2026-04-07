package com.drf.coupon.discount;

import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;
import org.springframework.stereotype.Component;

@Component
public class FixedDiscountPolicy implements DiscountPolicy {

    @Override
    public DiscountType getType() {
        return DiscountType.FIXED;
    }

    @Override
    public int calculate(Coupon coupon, int base) {
        return coupon.getDiscountValue();
    }
}
