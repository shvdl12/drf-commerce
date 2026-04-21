package com.drf.member.model.response;

import com.drf.member.entitiy.DeliveryAddress;

public record InternalDeliveryAddressResponse(
        String receiverName,
        String phone,
        String zipCode,
        String address,
        String addressDetail
) {
    public static InternalDeliveryAddressResponse from(DeliveryAddress deliveryAddress) {
        return new InternalDeliveryAddressResponse(
                deliveryAddress.getName(),
                deliveryAddress.getPhone(),
                deliveryAddress.getZipCode(),
                deliveryAddress.getAddress(),
                deliveryAddress.getAddressDetail()
        );
    }
}
