package com.drf.common.model;

import com.drf.common.exception.errorcode.ErrorCodeSpec;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = CommonResponse.CommonResponseBuilder.class)
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

    public static <T> CommonResponse<T> failure(ErrorCodeSpec errorCode) {
        return CommonResponse.<T>builder()
                .code(errorCode.name())
                .message(errorCode.getMessage())
                .build();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class CommonResponseBuilder<T> {
        // Lombok @Builder generates the implementation
    }
}
