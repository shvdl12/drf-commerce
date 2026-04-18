package com.drf.coupon.model.response;

import com.drf.coupon.entity.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberCouponListResponse(
        long memberCouponId,
        String couponName,
        DiscountType discountType,
        int discountValue,
        Integer maxDiscountAmount,
        int minOrderAmount,
        Integer minOrderQuantity,
        ApplyType applyType,
        ApplyScope applyScope,
        Long applyTargetId,
        boolean isUnlimited,
        int maxIssuablePerMember,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        MemberCouponStatus status,
        LocalDateTime usedAt,
        LocalDateTime reservedAt
) {
    public static MemberCouponListResponse from(MemberCoupon memberCoupon) {
        Coupon coupon = memberCoupon.getCoupon();

        return MemberCouponListResponse.builder()
                .memberCouponId(memberCoupon.getId())
                .couponName(coupon.getName())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .minOrderQuantity(coupon.getMinOrderQuantity())
                .applyType(coupon.getApplyType())
                .applyScope(coupon.getApplyScope())
                .applyTargetId(coupon.getApplyTargetId())
                .isUnlimited(coupon.isUnlimited())
                .maxIssuablePerMember(coupon.getMaxIssuablePerMember())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .status(memberCoupon.getStatus())
                .usedAt(memberCoupon.getUsedAt())
                .reservedAt(memberCoupon.getReservedAt())
                .build();
    }
}