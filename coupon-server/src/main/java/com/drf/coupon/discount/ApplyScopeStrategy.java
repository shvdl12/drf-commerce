package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyScope;

public interface ApplyScopeStrategy {

    ApplyScope getApplyScope();

    int getBase(DiscountContext context);
}
