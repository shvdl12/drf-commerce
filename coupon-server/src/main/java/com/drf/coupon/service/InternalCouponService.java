package com.drf.coupon.service;

import com.drf.common.exception.BusinessException;
import com.drf.coupon.common.exception.ErrorCode;
import com.drf.coupon.entity.ApplyType;
import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.entity.MemberCouponStatus;
import com.drf.coupon.model.request.internal.InternalCouponBatchReserveRequest;
import com.drf.coupon.repository.MemberCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
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

    @Transactional
    public void batchReserveCoupon(List<InternalCouponBatchReserveRequest.InternalCouponBatchReserveItem> items) {
        for (var item : items) {
            if (memberCouponRepository.reserve(item.memberCouponId(), item.memberId(), LocalDateTime.now()) == 0) {
                throw new BusinessException(ErrorCode.COUPON_RESERVE_FAILED);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void batchReleaseCoupon(List<InternalCouponBatchReserveRequest.InternalCouponBatchReserveItem> items) {
        for (var item : items) {
            try {
                if (memberCouponRepository.release(item.memberCouponId(), item.memberId()) == 0) {
                    throw new BusinessException(ErrorCode.COUPON_RELEASE_FAILED);
                }
            } catch (Exception e) {
                log.error("Release failed for memberCouponId={}", item.memberCouponId(), e);
            }
        }
    }
}
