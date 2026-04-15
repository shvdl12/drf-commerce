package com.drf.order.repository;

import com.drf.order.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    List<Cart> findByMemberId(Long memberId);

    Optional<Cart> findByMemberIdAndProductId(Long memberId, Long productId);
}
