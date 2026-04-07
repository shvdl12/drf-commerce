package com.drf.coupon.controller;

import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.coupon.model.response.CouponCalculateResponse;
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

    @GetMapping("/members/me/coupons/{memberCouponId}/calculate")
    public ResponseEntity<CommonResponse<CouponCalculateResponse>> calculateCoupon(
            AuthInfo authInfo,
            @PathVariable Long memberCouponId,
            @RequestParam int orderAmount,
            @RequestParam(required = false) Integer categoryAmount) {
        CouponCalculateResponse response = couponFacade.calculateCoupon(authInfo.id(), memberCouponId, orderAmount, categoryAmount);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
