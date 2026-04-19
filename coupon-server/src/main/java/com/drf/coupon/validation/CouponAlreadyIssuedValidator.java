package com.drf.coupon.validation;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponAlreadyIssuedValidator implements CouponValidationStrategy {

    private final MemberCouponRepository memberCouponRepository;

    @Override
    public boolean supports(ValidationType type) {
        return type == ValidationType.ISSUE;
    }

    @Override
    public void validate(CouponValidationContext context) {
        long issuedCount = memberCouponRepository.countByMemberIdAndCouponId(
                context.memberId(), context.coupon().getId());
        if (issuedCount >= context.coupon().getMaxIssuablePerMember()) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }
}
