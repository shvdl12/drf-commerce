package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.client.CouponClient;
import com.drf.order.client.dto.request.InternalCartCouponItemRequest;
import com.drf.order.client.dto.request.InternalCartCouponRequest;
import com.drf.order.client.dto.request.InternalProductCouponRequest;
import com.drf.order.client.dto.response.InternalCartCouponAvailableListResponse;
import com.drf.order.client.dto.response.InternalCartCouponCalculateResponse;
import com.drf.order.client.dto.response.InternalProductCouponListResponse;
import com.drf.order.client.dto.response.ProductCouponCalculateResponse;
import com.drf.order.common.exception.ErrorCode;
import com.drf.order.model.request.CartCouponAvailableRequest;
import com.drf.order.model.request.ProductCouponApplyRequest;
import com.drf.order.model.request.ProductCouponAvailableRequest;
import com.drf.order.model.response.ProductCouponApplyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponFacade {

    private final CouponClient couponClient;
    private final CartService cartService;

    public InternalCartCouponAvailableListResponse getAvailableCartCoupons(List<CartCouponAvailableRequest> items, long memberId) {
        List<InternalCartCouponItemRequest> couponItems = toInternalRequests(items);
        return couponClient.getAvailableCartCoupons(new InternalCartCouponRequest(memberId, couponItems)).getData();
    }

    public InternalCartCouponCalculateResponse applyCartCoupon(long memberId, long memberCouponId, List<CartCouponAvailableRequest> items) {
        List<InternalCartCouponItemRequest> couponItems = toInternalRequests(items);
        InternalCartCouponCalculateResponse result = couponClient.calculateCartCoupon(
                memberCouponId, new InternalCartCouponRequest(memberId, couponItems)).getData();

        if (result.applicable()) {
            cartService.updateCartCoupon(memberId, memberCouponId);
        }

        return result;
    }

    public InternalProductCouponListResponse getAvailableProductCoupons(long memberId, ProductCouponAvailableRequest request) {
        List<Long> usedCouponIds = cartService.getUsedMemberCouponIds(memberId);
        InternalProductCouponRequest internalRequest = InternalProductCouponRequest.from(memberId, request, usedCouponIds);
        return couponClient.getAvailableProductCoupons(internalRequest).getData();
    }

    public ProductCouponApplyResponse applyProductCoupon(long memberId, long productId, long memberCouponId, ProductCouponApplyRequest request) {
        InternalProductCouponRequest couponRequest = new InternalProductCouponRequest(
                memberId, request.cartItemId(), productId, request.price(), request.quantity(), request.categoryPath(), List.of());
        ProductCouponCalculateResponse result = couponClient.calculateProductCoupon(memberCouponId, couponRequest).getData();

        if (!result.applicable()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_APPLICABLE);
        }

        cartService.updateCartItemCoupon(memberId, productId, memberCouponId);
        return new ProductCouponApplyResponse(result.discountAmount());
    }

    private List<InternalCartCouponItemRequest> toInternalRequests(List<CartCouponAvailableRequest> items) {
        return items.stream()
                .map(i -> new InternalCartCouponItemRequest(
                        i.cartItemId(), i.productId(), i.price(), i.quantity(), i.categoryPath()))
                .toList();
    }
}
