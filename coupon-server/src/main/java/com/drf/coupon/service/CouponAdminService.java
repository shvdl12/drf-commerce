package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.DiscountType;
import com.drf.coupon.model.request.CouponCreateRequest;
import com.drf.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponAdminService {

    private final CouponRepository couponRepository;

    @Transactional
    public Long createCoupon(CouponCreateRequest request) {
        validateCreateRequest(request);

        Coupon coupon = Coupon.create(
                request.name(),
                request.discountType(),
                request.discountValue(),
                request.totalQuantity(),
                request.minOrderAmount(),
                request.maxDiscountAmount(),
                request.applyType(),
                request.applyTargetId(),
                request.validFrom(),
                request.validUntil()
        );

        return couponRepository.save(coupon).getId();
    }

    private void validateCreateRequest(CouponCreateRequest request) {
        if (!request.validUntil().isAfter(request.validFrom())) {
            throw new BusinessException(ErrorCode.INVALID_VALID_DATE_RANGE);
        }
        if (request.applyType() == ApplyType.CATEGORY && request.applyTargetId() == null) {
            throw new BusinessException(ErrorCode.CATEGORY_COUPON_REQUIRES_TARGET);
        }
        if (request.discountType() == DiscountType.RATE && request.maxDiscountAmount() == null) {
            throw new BusinessException(ErrorCode.RATE_COUPON_REQUIRES_MAX_DISCOUNT);
        }
    }
}
