package com.drf.member.controller;

import com.drf.common.model.CommonResponse;
import com.drf.member.model.response.InternalDeliveryAddressResponse;
import com.drf.member.service.DeliveryAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/delivery-addresses")
public class InternalDeliveryAddressController {

    private final DeliveryAddressService deliveryAddressService;

    @GetMapping("/{memberId}/{addressId}")
    public ResponseEntity<CommonResponse<InternalDeliveryAddressResponse>> getDeliveryAddress(
            @PathVariable long memberId,
            @PathVariable long addressId
    ) {
        return ResponseEntity.ok(CommonResponse.success(
                deliveryAddressService.findByIdAndMemberId(memberId, addressId)
        ));
    }
}
