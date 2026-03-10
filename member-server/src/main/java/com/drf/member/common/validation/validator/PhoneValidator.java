package com.drf.member.common.validation.validator;

import com.drf.member.common.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789]-\\d{3,4}-\\d{4}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return PHONE_PATTERN.matcher(value).matches();
    }
}
