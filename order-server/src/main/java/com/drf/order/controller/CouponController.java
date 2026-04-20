package com.drf.order.controller;

import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.response.InternalCartCouponAvailableListResponse;
import com.drf.order.client.dto.response.InternalCartCouponCalculateResponse;
import com.drf.order.client.dto.response.InternalProductCouponListResponse;
import com.drf.order.model.request.CartCouponAvailableRequest;
import com.drf.order.model.request.ProductCouponApplyRequest;
import com.drf.order.model.request.ProductCouponAvailableRequest;
import com.drf.order.model.response.ProductCouponApplyResponse;
import com.drf.order.service.CouponFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponFacade couponFacade;

    @PostMapping("/cart/available")
    public ResponseEntity<CommonResponse<InternalCartCouponAvailableListResponse>> getAvailableCartCoupons(
            @RequestBody List<CartCouponAvailableRequest> items,
            AuthInfo authInfo) {
        return ResponseEntity.ok(CommonResponse.success(
                couponFacade.getAvailableCartCoupons(items, authInfo.id())));
    }

    @PostMapping("/cart/{memberCouponId}")
    public ResponseEntity<CommonResponse<InternalCartCouponCalculateResponse>> applyCartCoupon(
            @PathVariable long memberCouponId,
            @RequestBody List<CartCouponAvailableRequest> items,
            AuthInfo authInfo) {
        return ResponseEntity.ok(CommonResponse.success(
                couponFacade.applyCartCoupon(authInfo.id(), memberCouponId, items)));
    }

    @PostMapping("/products/{productId}/available")
    public ResponseEntity<CommonResponse<InternalProductCouponListResponse>> getAvailableProductCoupons(
            @Valid @RequestBody ProductCouponAvailableRequest request,
            AuthInfo authInfo) {
        return ResponseEntity.ok(CommonResponse.success(
                couponFacade.getAvailableProductCoupons(authInfo.id(), request)));
    }

    @PostMapping("/products/{productId}/{memberCouponId}")
    public ResponseEntity<CommonResponse<ProductCouponApplyResponse>> applyProductCoupon(
            @PathVariable long productId,
            @PathVariable long memberCouponId,
            @RequestBody ProductCouponApplyRequest request,
            AuthInfo authInfo) {
        return ResponseEntity.ok(CommonResponse.success(
                couponFacade.applyProductCoupon(authInfo.id(), productId, memberCouponId, request)));
    }
}
