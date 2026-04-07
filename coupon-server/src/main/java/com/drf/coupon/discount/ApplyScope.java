package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyType;

public interface ApplyScope {

    ApplyType getType();

    int getBase(DiscountContext context);
}
