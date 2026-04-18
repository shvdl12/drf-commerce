package com.drf.coupon.service;

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
}
