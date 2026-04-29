package com.drf.order.service;

import com.drf.order.client.CouponClient;
import com.drf.order.client.dto.request.CouponBatchReserveRequest;
import com.drf.order.client.dto.request.InternalCartCouponItemRequest;
import com.drf.order.client.dto.request.InternalCartCouponRequest;
import com.drf.order.client.dto.request.InternalProductCouponRequest;
import com.drf.order.client.dto.response.InternalCartCouponCalculateResponse;
import com.drf.order.client.dto.response.InternalCouponItemResult;
import com.drf.order.client.dto.response.ProductCouponCalculateResponse;
import com.drf.order.entity.Cart;
import com.drf.order.model.dto.OrderLineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCouponService {

    private final CouponClient couponClient;


    public void calculateDiscounts(long memberId, Cart cart, List<OrderLineItem> lineItems) {
        calculateProductDiscounts(memberId, lineItems);

        if (cart.getCouponId() != null) {
            calculateCartDiscount(memberId, cart, lineItems);
        }
    }

    public List<Long> collectCouponIds(Cart cart, List<OrderLineItem> lineItems) {
        Set<Long> ids = new HashSet<>();

        if (cart.getCouponId() != null) {
            ids.add(cart.getCouponId());
        }

        for (OrderLineItem item : lineItems) {
            if (item.getMemberCouponId() != null) {
                ids.add(item.getMemberCouponId());
            }
        }

        return new ArrayList<>(ids);
    }

    private void calculateProductDiscounts(long memberId, List<OrderLineItem> lineItems) {
        for (OrderLineItem item : lineItems) {
            if (item.getMemberCouponId() == null) continue;

            InternalProductCouponRequest request = InternalProductCouponRequest.builder()
                    .memberId(memberId)
                    .cartItemId(item.getCartItemId())
                    .productId(item.getProductId())
                    .price(item.getDiscountedUnitPrice())
                    .quantity(item.getQuantity())
                    .categoryPath(item.getCategoryPath())
                    .usedMemberCouponIds(List.of())
                    .build();

            ProductCouponCalculateResponse response = couponClient
                    .calculateProductCoupon(item.getMemberCouponId(), request).getData();

            if (response.applicable()) {
                item.applyProductCouponDiscount(response.discountAmount());
            }
        }
    }

    private void calculateCartDiscount(long memberId, Cart cart, List<OrderLineItem> lineItems) {
        List<InternalCartCouponItemRequest> items = lineItems.stream()
                .map(item -> InternalCartCouponItemRequest.builder()
                        .cartItemId(item.getCartItemId())
                        .productId(item.getProductId())
                        .lineAmount(item.getLineAmount())
                        .quantity(item.getQuantity())
                        .categoryPath(item.getCategoryPath())
                        .build())
                .toList();

        InternalCartCouponCalculateResponse r = couponClient
                .calculateCartCoupon(cart.getCouponId(), new InternalCartCouponRequest(memberId, items)).getData();

        if (!r.applicable()) {
            return;
        }

        Map<Long, OrderLineItem> lineItemMap = lineItems.stream()
                .collect(Collectors.toMap(OrderLineItem::getCartItemId, Function.identity()));

        for (InternalCouponItemResult result : r.items()) {
            if (result.appliedYn()) {
                OrderLineItem orderLineItem = lineItemMap.get(result.cartItemId());
                orderLineItem.applyOrderCouponDiscount(result.discountAmount());
            }
        }
    }

    public void reserveCoupons(List<Long> memberCouponIds, long memberId) {
        if (memberCouponIds.isEmpty()) return;

        couponClient.reserveCoupon(new CouponBatchReserveRequest(memberCouponIds.stream()
                .map(id -> new CouponBatchReserveRequest.CouponBatchReserveItem(id, memberId))
                .toList()));
    }

    public void releaseCoupons(List<Long> memberCouponIds, long memberId) {
        if (memberCouponIds.isEmpty()) return;

        try {
            couponClient.releaseCoupon(new CouponBatchReserveRequest(memberCouponIds.stream()
                    .map(id -> new CouponBatchReserveRequest.CouponBatchReserveItem(id, memberId))
                    .toList()));
        } catch (Exception e) {
            log.error("Coupon batch release failed", e);
        }
    }
}
