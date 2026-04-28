package com.drf.coupon.discount;

import com.drf.common.money.Money;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;
import org.springframework.stereotype.Component;

@Component
public class RateDiscountStrategy implements DiscountStrategy {

    @Override
    public DiscountType getType() {
        return DiscountType.RATE;
    }

    @Override
    public Money calculate(Coupon coupon, Money applicableAmount) {
        Money discount = applicableAmount
                .percent(coupon.getDiscountValue())
                .truncateTo(10);

        return coupon.getMaxDiscountAmount() != null
                ? discount.cap(Money.of(coupon.getMaxDiscountAmount()))
                : discount;
    }
}
