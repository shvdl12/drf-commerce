package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyScope;
import org.springframework.stereotype.Component;

@Component
public class OrderApplyScope implements ApplyScopeStrategy {

    @Override
    public ApplyScope getApplyScope() {
        return ApplyScope.ALL;
    }

    @Override
    public int getBase(DiscountContext context) {
        return context.orderAmount();
    }
}
