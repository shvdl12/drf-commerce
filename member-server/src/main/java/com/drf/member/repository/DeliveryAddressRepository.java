package com.drf.member.repository;

import com.drf.member.entitiy.DeliveryAddress;
import com.drf.member.entitiy.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {

    boolean existsByMember(Member member);

    Optional<DeliveryAddress> findByMemberAndIsDefaultTrue(Member member);

    List<DeliveryAddress> findByMemberOrderByIdDesc(Member member);

    Optional<DeliveryAddress> findByIdAndMemberId(Long addressId, Long memberId);
}
