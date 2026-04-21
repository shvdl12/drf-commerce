package com.drf.order.client.dto.response;

public record DeliveryAddressResponse(
        String receiverName,
        String phone,
        String zipCode,
        String address,
        String addressDetail
) {
}
