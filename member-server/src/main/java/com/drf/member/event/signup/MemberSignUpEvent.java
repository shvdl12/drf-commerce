package com.drf.member.event.signup;

import com.drf.member.event.base.BaseEvent;
import com.drf.member.event.base.EventType;

public class MemberSignUpEvent extends BaseEvent<MemberSignUpEvent.Payload> {

    public MemberSignUpEvent(Long id) {
        super(EventType.MEMBER_SIGN_UP, new MemberSignUpEvent.Payload(id));
    }

    public record Payload(long id) {
    }
}


