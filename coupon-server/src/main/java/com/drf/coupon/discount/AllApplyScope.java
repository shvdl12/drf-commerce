package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyType;
import org.springframework.stereotype.Component;

@Component
public class AllApplyScope implements ApplyScope {

    @Override
    public ApplyType getType() {
        return ApplyType.ALL;
    }

    @Override
    public int getBase(DiscountContext context) {
        return context.orderAmount();
    }
}
