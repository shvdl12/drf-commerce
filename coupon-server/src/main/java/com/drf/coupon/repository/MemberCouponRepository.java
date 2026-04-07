package com.drf.coupon.repository;

import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.entity.MemberCouponStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    @EntityGraph(attributePaths = {"coupon"})
    List<MemberCoupon> findByMemberIdAndStatus(Long memberId, MemberCouponStatus status);

    @EntityGraph(attributePaths = {"coupon"})
    Optional<MemberCoupon> findByIdAndMemberIdAndStatus(Long id, Long memberId, MemberCouponStatus status);

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);
}
