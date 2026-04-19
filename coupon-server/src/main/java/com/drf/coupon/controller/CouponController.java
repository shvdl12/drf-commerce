package com.drf.coupon.controller;

import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.coupon.model.response.CouponIssueResponse;
import com.drf.coupon.model.response.MemberCouponListResponse;
import com.drf.coupon.service.CouponFacade;
import com.drf.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponFacade couponFacade;

    @PostMapping("/members/me/coupons/{couponId}")
    public ResponseEntity<CommonResponse<CouponIssueResponse>> issueCoupon(
            AuthInfo authInfo, @PathVariable Long couponId) {
        CouponIssueResponse response = couponFacade.issueCoupon(authInfo.id(), couponId);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @GetMapping("/members/me/coupons")
    public ResponseEntity<CommonResponse<List<MemberCouponListResponse>>> getMemberCoupons(AuthInfo authInfo) {
        List<MemberCouponListResponse> response = couponService.getMemberCoupons(authInfo.id());
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PatchMapping("/members/me/coupons/{memberCouponId}/reserve")
    public ResponseEntity<CommonResponse<Void>> reserveCoupon(
            AuthInfo authInfo, @PathVariable Long memberCouponId) {
        couponService.reserveCoupon(memberCouponId, authInfo.id());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/members/me/coupons/{memberCouponId}/release")
    public ResponseEntity<CommonResponse<Void>> releaseCoupon(
            AuthInfo authInfo, @PathVariable Long memberCouponId) {
        couponService.releaseCoupon(memberCouponId, authInfo.id());
        return ResponseEntity.ok().build();
    }
}
