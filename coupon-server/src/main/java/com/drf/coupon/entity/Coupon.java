package com.drf.coupon.entity;

import com.drf.common.entity.BaseTimeEntity;
import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "coupon")
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    @Column(nullable = false)
    private int discountValue;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private int minOrderAmount;

    private Integer maxDiscountAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplyType applyType;

    private Long applyTargetId;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    private LocalDateTime deletedAt;

    public void update(String name, DiscountType discountType, int discountValue,
                       int totalQuantity, int minOrderAmount, Integer maxDiscountAmount,
                       ApplyType applyType, Long applyTargetId,
                       LocalDateTime validFrom, LocalDateTime validUntil) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.totalQuantity = totalQuantity;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.applyType = applyType;
        this.applyTargetId = applyTargetId;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public void delete() {
        this.status = CouponStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public static Coupon create(String name, DiscountType discountType, int discountValue,
                                int totalQuantity, int minOrderAmount, Integer maxDiscountAmount,
                                ApplyType applyType, Long applyTargetId,
                                LocalDateTime validFrom, LocalDateTime validUntil) {
        return Coupon.builder()
                .name(name)
                .discountType(discountType)
                .discountValue(discountValue)
                .totalQuantity(totalQuantity)
                .issuedQuantity(0)
                .minOrderAmount(minOrderAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .applyType(applyType)
                .applyTargetId(applyTargetId)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .status(CouponStatus.ACTIVE)
                .build();
    }

    public void validateCouponAvailability() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(validFrom) || now.isAfter(validUntil)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
    }
}
