package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyType;
import org.springframework.stereotype.Component;

@Component
public class CategoryApplyScope implements ApplyScope {

    @Override
    public ApplyType getType() {
        return ApplyType.CATEGORY;
    }

    @Override
    public int getBase(DiscountContext context) {
        return context.categoryAmount();
    }
}
