package com.drf.member.common.validation.annotation;

import com.drf.member.common.validation.validator.PhoneValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
@Documented
public @interface ValidPhone {
    String message() default "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
