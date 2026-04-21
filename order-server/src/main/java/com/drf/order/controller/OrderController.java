package com.drf.order.controller;

import com.drf.common.idempotency.Idempotent;
import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.order.facade.OrderFacade;
import com.drf.order.model.request.OrderCreateRequest;
import com.drf.order.model.response.OrderCreateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping("/orders")
    @Idempotent(scope = "ORDER_CREATE")
    public ResponseEntity<CommonResponse<OrderCreateResponse>> createOrder(
            AuthInfo authInfo,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderCreateResponse response = orderFacade.createOrder(authInfo, idempotencyKey, request);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
