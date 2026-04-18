package com.drf.order.service;

import com.drf.order.client.CouponClient;
import com.drf.order.client.dto.request.InternalCartCouponItemRequest;
import com.drf.order.client.dto.request.InternalCartCouponRequest;
import com.drf.order.client.dto.response.InternalCartCouponAvilableListResponse;
import com.drf.order.model.request.CartCouponAvailableRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartCouponService {

    private final CouponClient couponClient;

    public InternalCartCouponAvilableListResponse getAvailableCartCoupons(List<CartCouponAvailableRequest> items, long memberId) {
        List<InternalCartCouponItemRequest> couponItems = items.stream()
                .map(i -> new InternalCartCouponItemRequest(
                        i.cartItemId(), i.productId(), i.price(), i.quantity(), i.categoryPath()))
                .toList();

        return couponClient.getAvailableCartCoupons(new InternalCartCouponRequest(memberId, couponItems)).getData();
    }
}
