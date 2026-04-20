package com.drf.order.client;

import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.request.InternalCartCouponRequest;
import com.drf.order.client.dto.response.InternalCartCouponAvilableListResponse;
import com.drf.order.client.dto.response.InternalCartCouponCalculateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coupon-client", url = "${clients.coupon-server.url}")
public interface CouponClient {

    @PostMapping("/internal/coupons/cart/available")
    CommonResponse<InternalCartCouponAvilableListResponse> getAvailableCartCoupons(@RequestBody InternalCartCouponRequest request);

    @PostMapping("/internal/coupons/cart/{memberCouponId}/calculate")
    CommonResponse<InternalCartCouponCalculateResponse> calculateCartCoupon(
            @PathVariable long memberCouponId,
            @RequestBody InternalCartCouponRequest request);
}
