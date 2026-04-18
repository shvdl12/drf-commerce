package com.drf.order.controller;

import com.drf.common.model.AuthInfo;
import com.drf.common.model.CommonResponse;
import com.drf.order.model.request.CartAddRequest;
import com.drf.order.model.request.CartUpdateRequest;
import com.drf.order.model.response.CartItemResponse;
import com.drf.order.service.CartFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartFacade cartFacade;

    @PostMapping("/members/me/cart/items")
    public ResponseEntity<Void> addItem(
            AuthInfo authInfo,
            @Valid @RequestBody CartAddRequest request) {
        cartFacade.addItem(authInfo.id(), request);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/members/me/cart/items/{cartItemId}")
    public ResponseEntity<Void> updateQuantity(
            AuthInfo authInfo,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartUpdateRequest request) {
        cartFacade.updateQuantity(authInfo.id(), cartItemId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/members/me/cart/items/{cartItemId}")
    public ResponseEntity<Void> removeItem(
            AuthInfo authInfo,
            @PathVariable Long cartItemId) {
        cartFacade.removeItem(authInfo.id(), cartItemId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/members/me/cart")
    public ResponseEntity<CommonResponse<List<CartItemResponse>>> getCart(AuthInfo authInfo) {
        List<CartItemResponse> response = cartFacade.getCart(authInfo.id());
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
