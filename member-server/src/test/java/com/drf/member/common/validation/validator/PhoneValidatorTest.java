package com.drf.member.common.validation.validator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class PhoneValidatorTest {
    private final PhoneValidator validator = new PhoneValidator();

    private static Stream<Arguments> validatePhoneParams() {
        return Stream.of(
                Arguments.of("010-1234-5678", true),  // 통과
                Arguments.of(null, true),             // null 통과

                Arguments.of("01012345678", false),   // 하이픈(-) 누락
                Arguments.of("02-1234-5678", false),  // 잘못된 앞자리
                Arguments.of("010-12-5678", false),   // 중간 자리수 이상
                Arguments.of("010-1234-567", false),  // 끝 자리수 이상
                Arguments.of("010-123a-5678", false), // 영문자 존재
                Arguments.of("010- 234-5678", false)  // 공백 존재
        );
    }

    @ParameterizedTest
    @MethodSource("validatePhoneParams")
    void validatePhone(String password, boolean expected) {
        // when
        boolean result = validator.isValid(password, null);

        // //then
        assertThat(result).isEqualTo(expected);
    }
}
