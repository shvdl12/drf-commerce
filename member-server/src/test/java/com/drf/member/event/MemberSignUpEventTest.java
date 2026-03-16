package com.drf.member.event;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MemberSignUpEventTest {

    @Test
    void createMemberSignUpEvent() {
        // when
        MemberSignUpEvent event = new MemberSignUpEvent(1L);

        // then
        assertThat(event.getEventId()).hasSize(13);
        assertThat(event.getEventType()).isEqualTo("MEMBER_SIGN_UP");
        assertThat(event.getPayload().id()).isEqualTo(1L);
        assertThat(event.getOccurredAt()).isNotNull();
    }
}
