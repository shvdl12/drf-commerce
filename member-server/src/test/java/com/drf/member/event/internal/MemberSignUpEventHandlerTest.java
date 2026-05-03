package com.drf.member.event.internal;

import com.drf.common.outbox.OutboxEventRepository;
import com.drf.common.util.JsonConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MemberSignUpEventHandlerTest {

    @InjectMocks
    private MemberEventHandler handler;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JsonConverter jsonConverter;

    @Test
    @DisplayName("회원가입 이벤트 발생 시 아웃박스 테이블에 이벤트를 저장한다")
    void handle_success() {
        // given
        MemberSignUpEvent event = new MemberSignUpEvent(1L);
        given(jsonConverter.toJson(event)).willReturn("{\"id\":1}");

        // when
        handler.handle(event);

        // then
        then(outboxEventRepository).should().save(any());
    }
}