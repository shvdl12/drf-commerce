package com.drf.coupon.controller;

import com.drf.common.model.CommonResponse;
import com.drf.coupon.model.request.CouponCreateRequest;
import com.drf.coupon.model.response.CouponCreateResponse;
import com.drf.coupon.service.CouponAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponAdminService couponAdminService;

    @PostMapping("/admin/coupons")
    public ResponseEntity<CommonResponse<CouponCreateResponse>> createCoupon(
            @Valid @RequestBody CouponCreateRequest request) {
        Long couponId = couponAdminService.createCoupon(request);
        return ResponseEntity.ok(CommonResponse.success(new CouponCreateResponse(couponId)));
    }
}
