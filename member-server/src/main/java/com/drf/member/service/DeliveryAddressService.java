package com.drf.member.service;

import com.drf.member.common.exception.BusinessException;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.common.model.AuthInfo;
import com.drf.member.entitiy.DeliveryAddress;
import com.drf.member.entitiy.Member;
import com.drf.member.model.request.DeliveryAddressCreateRequest;
import com.drf.member.model.request.DeliveryAddressUpdateRequest;
import com.drf.member.model.response.DeliveryAddressResponse;
import com.drf.member.repository.DeliveryAddressRepository;
import com.drf.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class DeliveryAddressService {

    private final DeliveryAddressRepository deliveryAddressRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public void register(DeliveryAddressCreateRequest request, AuthInfo authInfo) {
        Member member = memberRepository.findById(authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isDefault = request.isDefault() || !deliveryAddressRepository.existsByMember(member);

        if (isDefault) {
            deliveryAddressRepository.findByMemberAndIsDefaultTrue(member)
                    .ifPresent(DeliveryAddress::unmarkDefault);
        }

        DeliveryAddress deliveryAddress = DeliveryAddress.builder()
                .member(member)
                .name(request.name())
                .phone(request.phone())
                .address(request.address())
                .addressDetail(request.addressDetail())
                .zipCode(request.zipCode())
                .isDefault(isDefault)
                .build();

        deliveryAddressRepository.save(deliveryAddress);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAddressResponse> getDeliveryAddresses(AuthInfo authInfo) {
        Member member = memberRepository.findById(authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return deliveryAddressRepository.findByMemberOrderByIdDesc(member)
                .stream()
                .map(DeliveryAddressResponse::from)
                .toList();
    }

    @Transactional
    public void updateDeliveryAddress(Long addressId, DeliveryAddressUpdateRequest request, AuthInfo authInfo) {
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ADDRESS_NOT_FOUND));

        if (!deliveryAddress.getMember().getId().equals(authInfo.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (request.isDefault()) {
            deliveryAddressRepository.findByMemberAndIsDefaultTrue(deliveryAddress.getMember())
                    .ifPresent(DeliveryAddress::unmarkDefault);
        }

        deliveryAddress.update(request.name(), request.phone(), request.address(), request.addressDetail(),
                request.zipCode(), request.isDefault());
    }
}
