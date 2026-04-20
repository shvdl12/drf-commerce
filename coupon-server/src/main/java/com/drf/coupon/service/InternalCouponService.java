package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.entity.MemberCouponStatus;
import com.drf.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalCouponService {

    private final MemberCouponRepository memberCouponRepository;

    @Transactional(readOnly = true)
    public List<MemberCoupon> getUnusedCouponsByType(long memberId, ApplyType applyType) {
        return memberCouponRepository.findByMemberIdAndStatusAndCouponApplyType(
                memberId, MemberCouponStatus.UNUSED, applyType);
    }

    @Transactional(readOnly = true)
    public MemberCoupon getUnusedMemberCoupon(long memberId, long memberCouponId) {
        return memberCouponRepository.findByIdAndMemberIdAndStatus(memberCouponId, memberId, MemberCouponStatus.UNUSED)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_COUPON_NOT_FOUND));
    }
}
