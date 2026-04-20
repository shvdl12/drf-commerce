package com.drf.order.service;

import com.drf.order.client.CouponClient;
import com.drf.order.client.dto.request.InternalCartCouponItemRequest;
import com.drf.order.client.dto.request.InternalCartCouponRequest;
import com.drf.order.client.dto.response.InternalCartCouponAvilableListResponse;
import com.drf.order.client.dto.response.InternalCartCouponCalculateResponse;
import com.drf.order.model.request.CartCouponAvailableRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartCouponService {

    private final CouponClient couponClient;
    private final CartService cartService;

    public InternalCartCouponAvilableListResponse getAvailableCartCoupons(List<CartCouponAvailableRequest> items, long memberId) {
        List<InternalCartCouponItemRequest> couponItems = toInternalRequests(items);
        return couponClient.getAvailableCartCoupons(new InternalCartCouponRequest(memberId, couponItems)).getData();
    }

    @Transactional
    public InternalCartCouponCalculateResponse applyCartCoupon(long memberId, long memberCouponId, List<CartCouponAvailableRequest> items) {
        List<InternalCartCouponItemRequest> couponItems = toInternalRequests(items);
        InternalCartCouponCalculateResponse result = couponClient.calculateCartCoupon(
                memberCouponId, new InternalCartCouponRequest(memberId, couponItems)).getData();

        if (result.applicable()) {
            cartService.updateCartCoupon(memberId, memberCouponId);
        }

        return result;
    }

    private List<InternalCartCouponItemRequest> toInternalRequests(List<CartCouponAvailableRequest> items) {
        return items.stream()
                .map(i -> new InternalCartCouponItemRequest(
                        i.cartItemId(), i.productId(), i.price(), i.quantity(), i.categoryPath()))
                .toList();
    }
}
