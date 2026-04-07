package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.discount.ApplyScopeRegistry;
import com.drf.coupon.discount.DiscountContext;
import com.drf.coupon.discount.DiscountPolicyRegistry;
import com.drf.coupon.entity.*;
import com.drf.coupon.model.response.CouponCalculateResponse;
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
    private final DiscountPolicyRegistry discountPolicyRegistry;
    private final ApplyScopeRegistry applyScopeRegistry;

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
        validateCouponAvailability(coupon);

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

    @Transactional(readOnly = true)
    public CouponCalculateResponse calculateCoupon(Long memberId, Long memberCouponId, int orderAmount, Integer categoryAmount) {
        MemberCoupon memberCoupon = memberCouponRepository.findByIdAndMemberIdAndStatus(memberCouponId, memberId, MemberCouponStatus.UNUSED)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_COUPON_NOT_FOUND));

        Coupon coupon = memberCoupon.getCoupon();

        // 유효 기간 검증
        validateCouponAvailability(coupon);

        // 최소 주문 금액 검증
        if (orderAmount < coupon.getMinOrderAmount()) {
            throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }

        // 카테고리 쿠폰일 경우 카테고리 합계 주문 금액 검증
        if (coupon.getApplyType() == ApplyType.CATEGORY && (categoryAmount == null || categoryAmount == 0)) {
            throw new BusinessException(ErrorCode.CATEGORY_AMOUNT_REQUIRED);
        }

        // 적용 정책, 할인 정책 별 전략 수행
        DiscountContext discountContext = new DiscountContext(orderAmount, categoryAmount);
        int base = applyScopeRegistry.get(coupon.getApplyType()).getBase(discountContext);
        int discountAmount = discountPolicyRegistry.get(coupon.getDiscountType()).calculate(coupon, base);

        return new CouponCalculateResponse(orderAmount, discountAmount, Math.max(0, orderAmount - discountAmount));
    }

    private void validateCouponAvailability(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
    }
}
