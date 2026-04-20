package com.drf.coupon.discount;

import com.drf.coupon.entity.ApplyScope;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;

import java.util.List;

public interface ApplyScopeStrategy {

    ApplyScope getApplyScope();

    List<InternalCartCouponItemRequest> filterApplicableItems(List<InternalCartCouponItemRequest> items, Long applyTargetId);

    default int computeApplicableAmount(List<InternalCartCouponItemRequest> items) {
        return items.stream().mapToInt(i -> i.price() * i.quantity()).sum();
    }

    default int computeApplicableQuantity(List<InternalCartCouponItemRequest> items) {
        return items.stream().mapToInt(InternalCartCouponItemRequest::quantity).sum();
    }
}
