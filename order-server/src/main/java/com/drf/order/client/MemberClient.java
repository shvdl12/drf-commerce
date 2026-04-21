package com.drf.order.client;

import com.drf.common.model.CommonResponse;
import com.drf.order.client.dto.response.DeliveryAddressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-client", url = "${clients.member-server.url}")
public interface MemberClient {

    @GetMapping("/internal/delivery-addresses/{memberId}/{addressId}")
    CommonResponse<DeliveryAddressResponse> getDeliveryAddress(
            @PathVariable long memberId,
            @PathVariable long addressId
    );
}
