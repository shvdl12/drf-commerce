package com.drf.member.common.validation.validator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class PasswordValidatorTest {
    private final PasswordValidator validator = new PasswordValidator();

    private static Stream<Arguments> validatePasswordParams() {
        return Stream.of(
                Arguments.of("Password1!", true),   // 통과
                Arguments.of(null, true),           // null 통과

                Arguments.of("pass12!", false),     // 길이 부족 (7자)
                Arguments.of("password123", false), // 특수문자 누락
                Arguments.of("Password!", false),   // 숫자 누락
                Arguments.of("Pass 123!", false),   // 공백 포함
                Arguments.of("Password123=", false) // 미허용 특수문자(=) 포함
        );
    }

    @ParameterizedTest
    @MethodSource("validatePasswordParams")
    void validatePassword(String password, boolean expected) {
        // when
        boolean result = validator.isValid(password, null);

        // //then
        assertThat(result).isEqualTo(expected);
    }
}
