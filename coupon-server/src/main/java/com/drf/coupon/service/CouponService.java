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

    @Transactional(readOnly = true)
    public Coupon getCouponForIssue(Long couponId, Long memberId) {
        Coupon coupon = couponRepository.findByIdAndStatus(couponId, CouponStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        coupon.validateCouponAvailability();

        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        return coupon;
    }

    @Transactional
    public CouponIssueResponse issueCoupon(Coupon coupon, Long memberId) {
        if (couponRepository.incrementIssuedQuantity(coupon.getId()) == 0) {
            throw new BusinessException(ErrorCode.COUPON_EXHAUSTED);
        }

        try {
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issue(coupon, memberId));
            return new CouponIssueResponse(memberCoupon.getId());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }
    }

    @Transactional(readOnly = true)
    public MemberCoupon getMemberCoupon(Long memberCouponId, Long memberId) {
        return memberCouponRepository.findByIdAndMemberIdAndStatus(memberCouponId, memberId, MemberCouponStatus.UNUSED)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_COUPON_NOT_FOUND));
    }

    public CouponCalculateResponse calculate(MemberCoupon memberCoupon, int orderAmount, Integer categoryAmount) {
        Coupon coupon = memberCoupon.getCoupon();

        coupon.validateCouponAvailability();

        if (orderAmount < coupon.getMinOrderAmount()) {
            throw new BusinessException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET);
        }

        if (coupon.getApplyType() == ApplyType.CATEGORY && (categoryAmount == null || categoryAmount == 0)) {
            throw new BusinessException(ErrorCode.CATEGORY_AMOUNT_REQUIRED);
        }

        DiscountContext discountContext = new DiscountContext(orderAmount, categoryAmount);
        int base = applyScopeRegistry.get(coupon.getApplyType()).getBase(discountContext);
        int discountAmount = discountPolicyRegistry.get(coupon.getDiscountType()).calculate(coupon, base);

        return new CouponCalculateResponse(orderAmount, discountAmount, Math.max(0, orderAmount - discountAmount));
    }
}
