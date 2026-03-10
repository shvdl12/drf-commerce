package com.drf.member.common.model;

import com.drf.member.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {
    private final String code;
    private final String message;
    private final T data;

    public static <T> CommonResponse<T> success(T data) {
        return CommonResponse.<T>builder()
                .data(data)
                .build();
    }

    public static <T> CommonResponse<T> failure(String code, String message) {
        return CommonResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }

    public static <T> CommonResponse<T> failure(ErrorCode errorCode) {
        return CommonResponse.<T>builder()
                .code(errorCode.name())
                .message(errorCode.getMessage())
                .build();
    }
}
