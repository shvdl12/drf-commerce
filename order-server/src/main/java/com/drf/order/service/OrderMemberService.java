package com.drf.order.service;

import com.drf.common.exception.BusinessException;
import com.drf.order.client.MemberClient;
import com.drf.order.client.dto.response.DeliveryAddressResponse;
import com.drf.order.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMemberService {

    private final MemberClient memberClient;

    public DeliveryAddressResponse getDeliveryAddress(Long memberId, Long shippingAddressId) {
        try {
            return memberClient.getDeliveryAddress(memberId, shippingAddressId).getData();
        } catch (Exception e) {
            log.error("Failed to fetch delivery address, memberId: {}, addressId: {}",
                    memberId, shippingAddressId, e);
            throw new BusinessException(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND);
        }
    }
}
