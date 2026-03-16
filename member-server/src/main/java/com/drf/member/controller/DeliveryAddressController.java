package com.drf.member.controller;

import com.drf.member.common.model.AuthInfo;
import com.drf.member.common.model.CommonResponse;
import com.drf.member.model.request.DeliveryAddressCreateRequest;
import com.drf.member.model.request.DeliveryAddressUpdateRequest;
import com.drf.member.model.response.DeliveryAddressResponse;
import com.drf.member.service.DeliveryAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members/me/delivery-addresses")
public class DeliveryAddressController {

    private final DeliveryAddressService deliveryAddressService;

    @PostMapping
    public ResponseEntity<Void> addDeliverAddress(
            @RequestBody @Valid DeliveryAddressCreateRequest request, AuthInfo authInfo) {
        deliveryAddressService.addDeliverAddress(request, authInfo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<DeliveryAddressResponse>>> getDeliveryAddresses(AuthInfo authInfo) {
        List<DeliveryAddressResponse> response = deliveryAddressService.getDeliveryAddresses(authInfo);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> getDeliveryAddresses(
            @PathVariable long id, @RequestBody @Valid DeliveryAddressUpdateRequest request, AuthInfo authInfo) {
        deliveryAddressService.updateDeliveryAddress(id, request, authInfo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryAddress(@PathVariable long id, AuthInfo authInfo) {
        deliveryAddressService.deleteDeliveryAddress(id, authInfo);
        return ResponseEntity.noContent().build();
    }
}