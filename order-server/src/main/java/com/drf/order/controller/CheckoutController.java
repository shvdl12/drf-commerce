package com.drf.order.controller;

import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.order.model.request.CheckoutRequest;
import com.drf.order.model.response.CheckoutResponse;
import com.drf.order.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/orders/checkout")
    public ResponseEntity<CommonResponse<CheckoutResponse>> checkout(
            AuthInfo authInfo,
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutService.checkout(request.items());
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
