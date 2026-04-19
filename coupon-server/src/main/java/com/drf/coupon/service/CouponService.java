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
import com.drf.coupon.validation.CouponValidationContext;
import com.drf.coupon.validation.CouponValidationStrategy;
import com.drf.coupon.validation.ValidationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final List<CouponValidationStrategy> validators;

    @Transactional(readOnly = true)
    public List<MemberCouponListResponse> getMemberCoupons(Long memberId) {
        return memberCouponRepository.findByMemberIdAndStatus(memberId, MemberCouponStatus.UNUSED).stream()
                .map(MemberCouponListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Coupon getCouponForIssue(Long couponId, Long memberId) {
        Coupon coupon = couponRepository.findByIdAndStatus(couponId, CouponStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        validate(ValidationType.ISSUE, CouponValidationContext.forIssue(coupon, memberId));

        return coupon;
    }

    @Transactional
    public CouponIssueResponse issueCoupon(Coupon coupon, Long memberId) {
        if (couponRepository.incrementIssuedQuantity(coupon.getId()) == 0) {
            throw new BusinessException(ErrorCode.COUPON_EXHAUSTED);
        }

        MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issue(coupon, memberId));
        return new CouponIssueResponse(memberCoupon.getId());
    }

    @Transactional(readOnly = true)
    public MemberCoupon getMemberCoupon(Long memberCouponId, Long memberId) {
        return memberCouponRepository.findByIdAndMemberIdAndStatus(memberCouponId, memberId, MemberCouponStatus.UNUSED)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_COUPON_NOT_FOUND));
    }

    @Transactional
    public void reserveCoupon(Long memberCouponId, Long memberId) {
        if (memberCouponRepository.reserve(memberCouponId, memberId, LocalDateTime.now()) == 0) {
            throw new BusinessException(ErrorCode.COUPON_RESERVE_FAILED);
        }
    }

    @Transactional
    public void releaseCoupon(Long memberCouponId, Long memberId) {
        if (memberCouponRepository.release(memberCouponId, memberId) == 0) {
            throw new BusinessException(ErrorCode.COUPON_RELEASE_FAILED);
        }
    }

    private void validate(ValidationType type, CouponValidationContext context) {
        validators.stream()
                .filter(v -> v.supports(type))
                .forEach(v -> v.validate(context));
    }
}
