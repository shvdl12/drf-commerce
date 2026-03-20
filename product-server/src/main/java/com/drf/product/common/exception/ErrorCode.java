package com.drf.product.common.exception;

import com.drf.common.exception.errorcode.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeSpec {

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    INCOMPLETE_SALE_DATE(HttpStatus.BAD_REQUEST, "판매 기간은 시작일과 종료일을 모두 입력해야 합니다."),
    INVALID_SALE_DATE_RANGE(HttpStatus.BAD_REQUEST, "판매 종료일은 시작일 이후여야 합니다."),

    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 존재하는 카테고리입니다.")
    ;
    private final HttpStatus status;
    private final String message;
}
