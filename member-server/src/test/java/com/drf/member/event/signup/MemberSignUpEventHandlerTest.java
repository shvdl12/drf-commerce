package com.drf.member.event.signup;

import com.drf.member.common.util.JsonConverter;
import com.drf.member.infrastructure.kafka.KafkaProducer;
import com.drf.member.infrastructure.kafka.KafkaTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberSignUpEventHandlerTest {

    @InjectMocks
    private MemberSignUpEventHandler handler;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private JsonConverter jsonConverter;

    @Test
    @DisplayName("회원가입 이벤트 발생 시 Kafka 메시지를 발행한다")
    void handle_success() {
        // given
        MemberSignUpEvent event = new MemberSignUpEvent(1L);
        given(jsonConverter.toJson(event)).willReturn("{\"id\":1}");

        // when
        handler.handle(event);

        // then
        then(kafkaProducer).should().sendMessage(
                eq(KafkaTopic.MEMBER.getName()),
                eq("1"),
                eq("{\"id\":1}"),
                any(Runnable.class)
        );
    }
}