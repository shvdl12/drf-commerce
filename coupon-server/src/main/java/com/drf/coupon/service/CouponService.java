package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.CouponStatus;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.entity.MemberCouponStatus;
import com.drf.coupon.model.response.CouponIssueResponse;
import com.drf.coupon.model.response.MemberCouponListResponse;
import com.drf.coupon.repository.CouponRepository;
import com.drf.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Transactional(readOnly = true)
    public List<MemberCouponListResponse> getMemberCoupons(Long memberId) {
        return memberCouponRepository.findByMemberIdAndStatus(memberId, MemberCouponStatus.UNUSED).stream()
                .map(MemberCouponListResponse::from)
                .toList();
    }

    @Transactional
    public CouponIssueResponse issueCoupon(Long memberId, Long couponId) {
        // 쿠폰 상태 검증
        Coupon coupon = couponRepository.findByIdAndStatus(couponId, CouponStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 유효 기간 검증
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        // 중복 발급 검증
        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 쿠폰 수량 차감 및 검증
        int updated = couponRepository.incrementIssuedQuantity(couponId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COUPON_EXHAUSTED);
        }

        MemberCoupon memberCoupon;
        try {
            memberCoupon = memberCouponRepository.save(MemberCoupon.issue(coupon, memberId));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        return new CouponIssueResponse(memberCoupon.getId());
    }
}
