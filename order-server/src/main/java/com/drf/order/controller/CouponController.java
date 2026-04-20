package com.drf.order.controller;

import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.response.InternalCartCouponAvilableListResponse;
import com.drf.order.client.dto.response.InternalCartCouponCalculateResponse;
import com.drf.order.model.request.CartCouponAvailableRequest;
import com.drf.order.service.CartCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CartCouponService cartCouponService;

    @PostMapping("/cart/available")
    public ResponseEntity<CommonResponse<InternalCartCouponAvilableListResponse>> getAvailableCartCoupons(
            @RequestBody List<CartCouponAvailableRequest> items,
            AuthInfo authInfo) {
        return ResponseEntity.ok(CommonResponse.success(
                cartCouponService.getAvailableCartCoupons(items, authInfo.id())));
    }

    @PostMapping("/cart/{memberCouponId}")
    public ResponseEntity<CommonResponse<InternalCartCouponCalculateResponse>> applyCartCoupon(
            @PathVariable long memberCouponId,
            @RequestBody List<CartCouponAvailableRequest> items,
            AuthInfo authInfo) {
        return ResponseEntity.ok(CommonResponse.success(
                cartCouponService.applyCartCoupon(authInfo.id(), memberCouponId, items)));
    }
}
