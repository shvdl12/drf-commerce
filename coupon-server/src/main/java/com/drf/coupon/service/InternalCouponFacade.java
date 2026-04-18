package com.drf.coupon.service;

import com.drf.coupon.calculator.CartCouponCalculator;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.model.request.internal.InternalCartCouponRequest;
import com.drf.coupon.model.response.internal.InternalCartCouponListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InternalCouponFacade {

    private final InternalCouponService internalCouponService;
    private final CartCouponCalculator cartCouponCalculator;

    public InternalCartCouponListResponse getAvailableCartCoupons(InternalCartCouponRequest request) {
        List<MemberCoupon> memberCoupons = internalCouponService.getUnusedCouponsByType(request.memberId(), ApplyType.ORDER);
        return cartCouponCalculator.calculate(memberCoupons, request.items());
    }
}
