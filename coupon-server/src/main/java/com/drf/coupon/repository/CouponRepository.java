package com.drf.coupon.repository;

import com.drf.coupon.entity.Coupon;
import com.drf.coupon.entity.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByStatusNot(CouponStatus status);

    Optional<Coupon> findByIdAndStatusNot(Long id, CouponStatus status);

    Optional<Coupon> findByIdAndStatus(Long id, CouponStatus status);

    @Modifying
    @Query("UPDATE Coupon c SET c.issuedQuantity = c.issuedQuantity + 1 WHERE c.id = :id AND c.issuedQuantity < c.totalQuantity")
    int incrementIssuedQuantity(@Param("id") Long id);
}
