package com.drf.order.client;

import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.request.InternalCartCouponRequest;
import com.drf.order.client.dto.request.InternalProductCouponRequest;
import com.drf.order.client.dto.response.InternalCartCouponAvailableListResponse;
import com.drf.order.client.dto.response.InternalCartCouponCalculateResponse;
import com.drf.order.client.dto.response.InternalProductCouponListResponse;
import com.drf.order.client.dto.response.ProductCouponCalculateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coupon-client", url = "${clients.coupon-server.url}")
public interface CouponClient {

    @PostMapping("/internal/coupons/cart/available")
    CommonResponse<InternalCartCouponAvailableListResponse> getAvailableCartCoupons(@RequestBody InternalCartCouponRequest request);

    @PostMapping("/internal/coupons/cart/{memberCouponId}/calculate")
    CommonResponse<InternalCartCouponCalculateResponse> calculateCartCoupon(
            @PathVariable long memberCouponId,
            @RequestBody InternalCartCouponRequest request);

    @PostMapping("/internal/coupons/products/available")
    CommonResponse<InternalProductCouponListResponse> getAvailableProductCoupons(@RequestBody InternalProductCouponRequest request);

    @PostMapping("/internal/coupons/products/{memberCouponId}/calculate")
    CommonResponse<ProductCouponCalculateResponse> calculateProductCoupon(
            @PathVariable long memberCouponId,
            @RequestBody InternalProductCouponRequest request);
}
