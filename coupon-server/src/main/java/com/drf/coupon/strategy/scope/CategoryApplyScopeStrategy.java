package com.drf.coupon.strategy.scope;

import com.drf.coupon.entity.ApplyScope;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryApplyScopeStrategy implements ApplyScopeStrategy {

    @Override
    public ApplyScope getApplyScope() {
        return ApplyScope.CATEGORY;
    }

    @Override
    public List<InternalCartCouponItemRequest> filterApplicableItems(List<InternalCartCouponItemRequest> items, Long applyTargetId) {
        return items.stream()
                .filter(i -> i.categoryPath().contains(applyTargetId))
                .toList();
    }
}
