package com.drf.coupon.controller;

import com.drf.common.model.CommonResponse;
import com.drf.coupon.model.request.internal.InternalCartCouponRequest;
import com.drf.coupon.model.response.internal.InternalCartCouponListResponse;
import com.drf.coupon.service.InternalCouponFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/coupons")
@RequiredArgsConstructor
public class InternalCouponController {

    private final InternalCouponFacade internalCouponFacade;

    @PostMapping("/cart/available")
    public ResponseEntity<CommonResponse<InternalCartCouponListResponse>> getAvailableCartCoupons(
            @RequestBody InternalCartCouponRequest request) {
        return ResponseEntity.ok(CommonResponse.success(internalCouponFacade.getAvailableCartCoupons(request)));
    }
}
