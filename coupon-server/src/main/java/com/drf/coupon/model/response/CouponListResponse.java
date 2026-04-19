package com.drf.coupon.model.response;

import com.drf.coupon.entity.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CouponListResponse(
        long couponId,
        String couponName,
        DiscountType discountType,
        int discountValue,
        int minOrderAmount,
        int minOrderQuantity,
        Integer maxDiscountAmount,
        ApplyType applyType,
        ApplyScope applyScope,
        Long applyTargetId,
        boolean isUnlimited,
        int maxIssuablePerMember,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        int totalQuantity,
        int issuedQuantity,
        CouponStatus status,
        LocalDateTime deletedAt
) {
    public static CouponListResponse from(Coupon coupon) {
        return CouponListResponse.builder()
                .couponId(coupon.getId())
                .couponName(coupon.getName())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .totalQuantity(coupon.getTotalQuantity())
                .issuedQuantity(coupon.getIssuedQuantity())
                .minOrderAmount(coupon.getMinOrderAmount())
                .minOrderQuantity(coupon.getMinOrderQuantity())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .applyType(coupon.getApplyType())
                .applyScope(coupon.getApplyScope())
                .applyTargetId(coupon.getApplyTargetId())
                .isUnlimited(coupon.isUnlimited())
                .maxIssuablePerMember(coupon.getMaxIssuablePerMember())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .status(coupon.getStatus())
                .deletedAt(coupon.getDeletedAt())
                .build();
    }
}
