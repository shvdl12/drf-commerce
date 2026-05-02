package com.drf.coupon.facade;

import com.drf.coupon.entity.Coupon;
import com.drf.coupon.model.response.CouponIssueResponse;
import com.drf.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponFacade {

    private final CouponService couponService;

    public CouponIssueResponse issueCoupon(Long memberId, Long couponId) {
        Coupon coupon = couponService.getCouponForIssue(couponId, memberId);
        return couponService.issueCoupon(coupon, memberId);
    }
}
