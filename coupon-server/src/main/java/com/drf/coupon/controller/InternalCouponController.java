package com.drf.coupon.controller;

import com.drf.common.model.CommonResponse;
import com.drf.coupon.model.request.internal.InternalCartCouponRequest;
import com.drf.coupon.model.request.internal.InternalCouponBatchReserveRequest;
import com.drf.coupon.model.request.internal.InternalProductCouponRequest;
import com.drf.coupon.model.response.internal.InternalCartCouponCalculateResponse;
import com.drf.coupon.model.response.internal.InternalCartCouponListResponse;
import com.drf.coupon.model.response.internal.InternalProductCouponListResponse;
import com.drf.coupon.model.response.internal.ProductCouponCalculateResponse;
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

    @PostMapping("/cart/{memberCouponId}/calculate")
    public ResponseEntity<CommonResponse<InternalCartCouponCalculateResponse>> calculateCartCoupon(
            @PathVariable long memberCouponId,
            @RequestBody InternalCartCouponRequest request) {
        return ResponseEntity.ok(CommonResponse.success(
                internalCouponFacade.calculateCartCoupon(request.memberId(), memberCouponId, request.items())));
    }

    @PostMapping("/products/available")
    public ResponseEntity<CommonResponse<InternalProductCouponListResponse>> getAvailableProductCoupons(
            @RequestBody InternalProductCouponRequest request) {
        return ResponseEntity.ok(CommonResponse.success(internalCouponFacade.getAvailableProductCoupons(request)));
    }

    @PostMapping("/products/{memberCouponId}/calculate")
    public ResponseEntity<CommonResponse<ProductCouponCalculateResponse>> calculateProductCoupon(
            @PathVariable long memberCouponId,
            @RequestBody InternalProductCouponRequest request) {
        return ResponseEntity.ok(CommonResponse.success(
                internalCouponFacade.calculateProductCoupon(request.memberId(), memberCouponId, request)));
    }

    @PostMapping("/reserve")
    public ResponseEntity<CommonResponse<Void>> reserveCoupon(
            @RequestBody InternalCouponBatchReserveRequest request) {
        internalCouponFacade.batchReserveCoupon(request);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @DeleteMapping("/reserve")
    public ResponseEntity<CommonResponse<Void>> releaseCoupon(
            @RequestBody InternalCouponBatchReserveRequest request) {
        internalCouponFacade.batchReleaseCoupon(request);
        return ResponseEntity.ok(CommonResponse.success());
    }
}
