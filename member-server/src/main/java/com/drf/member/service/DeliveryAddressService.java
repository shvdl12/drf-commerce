package com.drf.member.service;

import com.drf.common.exception.BusinessException;
import com.drf.common.model.AuthInfo;
import com.drf.member.common.exception.ErrorCode;
import com.drf.member.entitiy.DeliveryAddress;
import com.drf.member.entitiy.Member;
import com.drf.member.model.request.DeliveryAddressCreateRequest;
import com.drf.member.model.request.DeliveryAddressUpdateRequest;
import com.drf.member.model.response.DeliveryAddressResponse;
import com.drf.member.model.response.InternalDeliveryAddressResponse;
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
    public void addDeliverAddress(DeliveryAddressCreateRequest request, AuthInfo authInfo) {
        Member member = memberRepository.findById(authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 기본 배송지 요청 혹은 첫 등록
        boolean isDefault = request.isDefault() || !deliveryAddressRepository.existsByMemberId(authInfo.id());

        if (isDefault) {
            deliveryAddressRepository.findByMemberIdAndIsDefaultTrue(authInfo.id())
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
        return deliveryAddressRepository.findByMemberIdOrderByIdDesc(authInfo.id())
                .stream()
                .map(DeliveryAddressResponse::from)
                .toList();
    }

    @Transactional
    public void updateDeliveryAddress(Long addressId, DeliveryAddressUpdateRequest request, AuthInfo authInfo) {
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findByIdAndMemberId(addressId, authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ADDRESS_NOT_FOUND));

        if (request.isDefault()) {
            deliveryAddressRepository.findByMemberIdAndIsDefaultTrue(authInfo.id())
                    .ifPresent(DeliveryAddress::unmarkDefault);
        }

        deliveryAddress.update(request.name(), request.phone(), request.address(), request.addressDetail(),
                request.zipCode(), request.isDefault());
    }

    @Transactional(readOnly = true)
    public InternalDeliveryAddressResponse findByIdAndMemberId(Long memberId, Long addressId) {
        DeliveryAddress address = deliveryAddressRepository.findByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ADDRESS_NOT_FOUND));
        return InternalDeliveryAddressResponse.from(address);
    }

    @Transactional
    public void deleteDeliveryAddress(Long addressId, AuthInfo authInfo) {
        DeliveryAddress deliveryAddress = deliveryAddressRepository.findByIdAndMemberId(addressId, authInfo.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.DELIVERY_ADDRESS_NOT_FOUND));

        if (deliveryAddress.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_ADDRESS_CANNOT_BE_DELETED);
        }

        deliveryAddressRepository.delete(deliveryAddress);
    }
}
