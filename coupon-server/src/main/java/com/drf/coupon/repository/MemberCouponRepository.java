package com.drf.coupon.repository;

import com.drf.coupon.entity.MemberCoupon;
import com.drf.coupon.entity.MemberCouponStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    @EntityGraph(attributePaths = {"coupon"})
    List<MemberCoupon> findByMemberIdAndStatus(Long memberId, MemberCouponStatus status);

    @EntityGraph(attributePaths = {"coupon"})
    Optional<MemberCoupon> findByIdAndMemberIdAndStatus(Long id, Long memberId, MemberCouponStatus status);

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

    @Modifying
    @Query("UPDATE MemberCoupon mc SET mc.status = 'RESERVED', mc.reservedAt = :now " +
            "WHERE mc.id = :id AND mc.memberId = :memberId AND mc.status = 'UNUSED'")
    int reserve(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE MemberCoupon mc SET mc.status = 'UNUSED', mc.reservedAt = null " +
            "WHERE mc.id = :id AND mc.memberId = :memberId AND mc.status = 'RESERVED'")
    int release(@Param("id") Long id, @Param("memberId") Long memberId);

    @Modifying
    @Query("UPDATE MemberCoupon mc SET mc.status = 'USED', mc.usedAt = :now " +
            "WHERE mc.id = :id AND mc.status = 'RESERVED'")
    int use(@Param("id") Long id, @Param("now") LocalDateTime now);
}
