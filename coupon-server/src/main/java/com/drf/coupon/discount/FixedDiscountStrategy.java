package com.drf.coupon.discount;

import com.drf.common.money.Money;
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
        // 쿠폰의 정액 할인 금액 그대로 리턴
        return Money.of(coupon.getDiscountValue());
    }
}
