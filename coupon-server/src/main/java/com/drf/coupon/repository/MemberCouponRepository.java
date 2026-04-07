package com.drf.coupon.repository;

import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.entity.MemberCouponStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    @EntityGraph(attributePaths = {"coupon"})
    List<MemberCoupon> findByMemberIdAndStatus(Long memberId, MemberCouponStatus status);

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);
}
