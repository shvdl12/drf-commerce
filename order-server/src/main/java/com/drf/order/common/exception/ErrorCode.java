package com.drf.order.common.exception;

import com.drf.common.exception.errorcode.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeSpec {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "장바구니에 추가할 수 없는 상품입니다."),
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니가 존재하지 않습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니에 없는 상품입니다."),
    COUPON_NOT_APPLICABLE(HttpStatus.BAD_REQUEST, "적용할 수 없는 쿠폰입니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
