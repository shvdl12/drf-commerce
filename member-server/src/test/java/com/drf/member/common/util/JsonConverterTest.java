package com.drf.member.common.util;

import com.drf.member.event.signup.MemberSignUpEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JsonTest
class JsonConverterTest {

    @Autowired
    private ObjectMapper objectMapper;

    private JsonConverter jsonConverter;

    @BeforeEach
    void setUp() {
        jsonConverter = new JsonConverter(objectMapper);
    }

    @Test
    @DisplayName("객체를 JSON 문자열로 직렬화한다")
    void toJson_success() {
        // given
        MemberSignUpEvent event = new MemberSignUpEvent(1L);

        // when
        String result = jsonConverter.toJson(event);

        // then
        assertThat(result).contains("\"eventType\":\"MEMBER_SIGN_UP\"");
        assertThat(result).contains("\"payload\":{\"id\":1}");
        assertThat(result).contains("\"eventId\":");
        assertThat(result).contains("\"occurredAt\":");
    }

    @Test
    @DisplayName("null 객체를 직렬화하면 예외가 발생한다")
    void toJson_null_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> jsonConverter.toJson(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("직렬화 실패 시 예외가 발생한다")
    void toJson_serializationFails_throwsRuntimeException() {
        Object circular = new Object() {
            public final Object self = this;
        };

        assertThatThrownBy(() -> jsonConverter.toJson(circular))
                .isInstanceOf(RuntimeException.class);
    }
}