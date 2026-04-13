package com.drf.coupon.common.exception;

import com.drf.common.exception.errorcode.ErrorCodeSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeSpec {
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰입니다."),
    COUPON_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "발급 가능한 기간이 아닙니다."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 쿠폰입니다."),
    COUPON_EXHAUSTED(HttpStatus.CONFLICT, "쿠폰 수량이 소진되었습니다."),
    MEMBER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "보유하지 않은 쿠폰입니다."),
    COUPON_MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "최소 주문 금액 조건을 충족하지 않습니다."),
    CATEGORY_AMOUNT_REQUIRED(HttpStatus.BAD_REQUEST, "카테고리 쿠폰은 카테고리 합계 금액이 필요합니다."),
    INVALID_VALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "쿠폰 유효기간 종료일은 시작일 이후여야 합니다."),
    CATEGORY_COUPON_REQUIRES_TARGET(HttpStatus.BAD_REQUEST, "카테고리 쿠폰은 적용 대상 카테고리를 지정해야 합니다."),
    RATE_COUPON_REQUIRES_MAX_DISCOUNT(HttpStatus.BAD_REQUEST, "정률 쿠폰은 최대 할인 금액을 지정해야 합니다."),
    COUPON_RESERVE_FAILED(HttpStatus.CONFLICT, "쿠폰을 선점할 수 없습니다."),
    COUPON_RELEASE_FAILED(HttpStatus.CONFLICT, "쿠폰 선점을 해제할 수 없습니다."),
    ;
    private final HttpStatus status;
    private final String message;
}
