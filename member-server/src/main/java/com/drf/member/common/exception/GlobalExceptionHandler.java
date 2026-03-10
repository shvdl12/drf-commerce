package com.drf.member.common.exception;

import com.drf.member.common.model.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 에러
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    // 405 에러
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodError(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    // JSON 파싱 실패
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleBadRequestError(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().build();
    }

    // 415 에러
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<CommonResponse<?>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(CommonResponse.builder().message(e.getMessage()).build());
    }

    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CommonResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError firstError = e.getBindingResult().getFieldErrors().getFirst();
        String message = String.format("[%s] %s", firstError.getField(), firstError.getDefaultMessage());

        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;

        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.failure(errorCode.name(), message));
    }

    // 비즈니스 에러
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<CommonResponse<?>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.failure(errorCode));
    }

    // 예외
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonResponse<?>> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(errorCode.getStatus())
                .body(CommonResponse.failure(errorCode));
    }
}