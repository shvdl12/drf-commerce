package com.drf.member.event.internal;

import com.drf.common.event.BaseEvent;
import com.drf.common.outbox.AggregateType;

public class MemberSignUpEvent extends BaseEvent<MemberSignUpEvent.Payload> {

    public MemberSignUpEvent(Long id) {
        super(AggregateType.MEMBER, MemberEventType.MEMBER_SIGNED_UP.name(), new MemberSignUpEvent.Payload(id));
    }

    public record Payload(long id) {
    }
}
