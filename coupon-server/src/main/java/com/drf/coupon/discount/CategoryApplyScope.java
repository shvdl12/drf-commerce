package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyScope;
import org.springframework.stereotype.Component;

@Component
public class CategoryApplyScope implements ApplyScopeStrategy {

    @Override
    public ApplyScope getApplyScope() {
        return ApplyScope.CATEGORY;
    }

    @Override
    public int getBase(DiscountContext context) {
        return context.categoryAmount();
    }
}
