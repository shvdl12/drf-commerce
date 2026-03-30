package com.drf.coupon.common.exception;

import com.drf.common.exception.errorcode.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeSpec {
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰입니다."),
    INVALID_VALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "쿠폰 유효기간 종료일은 시작일 이후여야 합니다."),
    CATEGORY_COUPON_REQUIRES_TARGET(HttpStatus.BAD_REQUEST, "카테고리 쿠폰은 적용 대상 카테고리를 지정해야 합니다."),
    RATE_COUPON_REQUIRES_MAX_DISCOUNT(HttpStatus.BAD_REQUEST, "정률 쿠폰은 최대 할인 금액을 지정해야 합니다."),
    ;
    private final HttpStatus status;
    private final String message;
}
