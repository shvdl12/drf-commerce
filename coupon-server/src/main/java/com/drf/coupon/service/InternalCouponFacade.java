package com.drf.coupon.service;

import com.drf.coupon.calculator.CartCouponCalculator;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;
import com.drf.coupon.model.request.internal.InternalCartCouponRequest;
import com.drf.coupon.model.response.internal.CartCouponResult;
import com.drf.coupon.model.response.internal.InternalCartCouponCalculateResponse;
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

    public InternalCartCouponCalculateResponse calculateCartCoupon(
            long memberId, long memberCouponId, List<InternalCartCouponItemRequest> items) {
        MemberCoupon memberCoupon = internalCouponService.getUnusedMemberCoupon(memberId, memberCouponId);
        InternalCartCouponListResponse result = cartCouponCalculator.calculate(List.of(memberCoupon), items);

        if (result.coupons().isEmpty()) {
            return new InternalCartCouponCalculateResponse(false, 0, List.of());
        }

        CartCouponResult couponResult = result.coupons().get(0);
        return new InternalCartCouponCalculateResponse(true, couponResult.getDiscountAmount(), couponResult.getItems());
    }
}
