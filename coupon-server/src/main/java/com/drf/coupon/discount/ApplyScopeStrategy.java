package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyScope;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;

import java.util.List;

public interface ApplyScopeStrategy {

    ApplyScope getApplyScope();

    List<InternalCartCouponItemRequest> filterApplicableItems(List<InternalCartCouponItemRequest> items, Long applyTargetId);

}
