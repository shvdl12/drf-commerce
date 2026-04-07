package com.drf.coupon.discount;

import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;
import org.springframework.stereotype.Component;

@Component
public class RateDiscountPolicy implements DiscountPolicy {

    @Override
    public DiscountType getType() {
        return DiscountType.RATE;
    }

    @Override
    public int calculate(Coupon coupon, int base) {
        int discount = base * coupon.getDiscountValue() / 100;
        return coupon.getMaxDiscountAmount() != null
                ? Math.min(discount, coupon.getMaxDiscountAmount())
                : discount;
    }
}
