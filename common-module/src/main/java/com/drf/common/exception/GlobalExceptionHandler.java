package com.drf.common.exception;

import com.drf.common.exception.errorcode.CommonErrorCode;
import com.drf.common.exception.errorcode.ErrorCodeSpec;
import com.drf.common.model.CommonResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodError(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleEnumError(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException ife) {
            if (ife.getTargetType().isEnum()) {
                String fieldName = ife.getPath().getFirst().getFieldName();
                String invalidValue = String.valueOf(ife.getValue());
                String message = String.format("[%s] %s", fieldName, "허용되지 않는 값입니다: " + invalidValue);

                return ResponseEntity.badRequest().body(
                        CommonResponse.failure(CommonErrorCode.INVALID_PARAMETER.name(), message)
                );
            }
        }

        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CommonResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldErrors().getFirst();
        String message = String.format("[%s] %s", firstError.getField(), firstError.getDefaultMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.failure(CommonErrorCode.INVALID_PARAMETER.name(), message));
    }

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<CommonResponse<?>> handleBusinessException(BusinessException e) {
        ErrorCodeSpec errorCode = e.getErrorCode();

        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.failure(errorCode));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    protected ResponseEntity<CommonResponse<?>> handleUnauthorizedAccessException(UnauthorizedAccessException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.failure(CommonErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonResponse<?>> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.failure(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
