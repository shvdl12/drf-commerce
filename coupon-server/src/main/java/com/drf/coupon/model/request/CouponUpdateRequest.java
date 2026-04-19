package com.drf.coupon.model.request;

import com.drf.coupon.entity.ApplyScope;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CouponUpdateRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotNull
        DiscountType discountType,

        @NotNull @Min(1)
        Integer discountValue,

        @NotNull @Min(1)
        Integer totalQuantity,

        @NotNull @Min(0)
        Integer minOrderAmount,

        @NotNull @Min(1)
        Integer minOrderQuantity,

        @Min(1)
        Integer maxDiscountAmount,

        @NotNull
        ApplyType applyType,

        @NotNull
        ApplyScope applyScope,

        Long applyTargetId,

        boolean isUnlimited,

        @NotNull @Min(1)
        Integer maxIssuablePerMember,

        @NotNull
        LocalDateTime validFrom,

        @NotNull
        LocalDateTime validUntil
) {
}
