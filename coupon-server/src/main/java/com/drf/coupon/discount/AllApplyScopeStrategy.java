package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyScope;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AllApplyScopeStrategy implements ApplyScopeStrategy {

    @Override
    public ApplyScope getApplyScope() {
        return ApplyScope.ALL;
    }

    @Override
    public List<InternalCartCouponItemRequest> filterApplicableItems(List<InternalCartCouponItemRequest> items, Long applyTargetId) {
        return items;
    }
}
