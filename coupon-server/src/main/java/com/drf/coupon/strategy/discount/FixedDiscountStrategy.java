package com.drf.coupon.strategy.discount;

import com.drf.common.model.Money;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;
import org.springframework.stereotype.Component;

@Component
public class FixedDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getType() {
        return DiscountType.FIXED;
    }

    @Override
    public Money calculate(Coupon coupon, Money applicableAmount) {
        return Money.of(coupon.getDiscountValue());
    }
}
