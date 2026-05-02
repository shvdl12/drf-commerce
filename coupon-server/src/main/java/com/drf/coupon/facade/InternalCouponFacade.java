package com.drf.coupon.facade;

import com.drf.coupon.calculator.CartCouponCalculator;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.model.request.internal.InternalCartCouponItemRequest;
import com.drf.coupon.model.request.internal.InternalCartCouponRequest;
import com.drf.coupon.model.request.internal.InternalCouponBatchReserveRequest;
import com.drf.coupon.model.request.internal.InternalProductCouponRequest;
import com.drf.coupon.model.response.internal.*;
import com.drf.coupon.service.InternalCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class InternalCouponFacade {

    private final InternalCouponService internalCouponService;
    private final CartCouponCalculator cartCouponCalculator;

    public InternalCartCouponListResponse getAvailableCartCoupons(InternalCartCouponRequest request) {
        List<MemberCoupon> memberCoupons = internalCouponService.getUnusedCouponsByType(request.memberId(), ApplyType.ORDER);
        return cartCouponCalculator.calculate(memberCoupons, request.items());
    }

    public InternalCartCouponCalculateResponse calculateCartCoupon(
            long memberId, long memberCouponId, List<InternalCartCouponItemRequest> items) {
        MemberCoupon memberCoupon = internalCouponService.getUnusedMemberCoupon(memberId, memberCouponId);
        InternalCartCouponListResponse result = cartCouponCalculator.calculate(List.of(memberCoupon), items);

        if (result.coupons().isEmpty()) {
            return new InternalCartCouponCalculateResponse(false, 0, List.of());
        }

        CartCouponResult couponResult = result.coupons().getFirst();
        return new InternalCartCouponCalculateResponse(true, couponResult.getDiscountAmount(), couponResult.getItems());
    }

    public ProductCouponCalculateResponse calculateProductCoupon(
            long memberId, long memberCouponId, InternalProductCouponRequest request) {
        MemberCoupon memberCoupon = internalCouponService.getUnusedMemberCoupon(memberId, memberCouponId);

        InternalCartCouponItemRequest item = new InternalCartCouponItemRequest(
                request.cartItemId(), request.productId(), request.price(), request.quantity(), request.categoryPath());

        InternalCartCouponListResponse result = cartCouponCalculator.calculate(List.of(memberCoupon), List.of(item));

        if (result.coupons().isEmpty()) {
            return new ProductCouponCalculateResponse(false, 0);
        }

        return new ProductCouponCalculateResponse(true, result.coupons().getFirst().getDiscountAmount());
    }

    public void batchReserveCoupon(InternalCouponBatchReserveRequest request) {
        internalCouponService.batchReserveCoupon(request.items());
    }

    public void batchReleaseCoupon(InternalCouponBatchReserveRequest request) {
        internalCouponService.batchReleaseCoupon(request.items());
    }

    public InternalProductCouponListResponse getAvailableProductCoupons(InternalProductCouponRequest request) {
        List<MemberCoupon> memberCoupons = internalCouponService.getUnusedCouponsByType(request.memberId(), ApplyType.PRODUCT);

        InternalCartCouponItemRequest item = new InternalCartCouponItemRequest(
                request.cartItemId(), request.productId(), request.price(), request.quantity(), request.categoryPath());

        InternalCartCouponListResponse result = cartCouponCalculator.calculate(memberCoupons, List.of(item));

        Set<Long> usedIds = Set.copyOf(request.usedMemberCouponIds());
        List<ProductCouponResult> coupons = result.coupons().stream()
                .map(cr -> new ProductCouponResult(
                        cr.getMemberCouponId(), cr.getName(), cr.getDiscountAmount(),
                        cr.isBest(), usedIds.contains(cr.getMemberCouponId())))
                .toList();

        return new InternalProductCouponListResponse(coupons);
    }
}
