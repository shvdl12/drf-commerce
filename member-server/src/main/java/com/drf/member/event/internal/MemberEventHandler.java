package com.drf.member.event.internal;

import com.drf.common.outbox.OutboxEvent;
import com.drf.common.outbox.OutboxEventRepository;
import com.drf.common.util.JsonConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventHandler {
    private final JsonConverter jsonConverter;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    @EventListener
    public void handle(MemberSignUpEvent event) {
        OutboxEvent outboxEvent = OutboxEvent.create(
                event.getEventId(),
                event.getAggregateType().name(),
                event.getEventType(),
                event.getAggregateType().getTopic(),
                jsonConverter.toJson(event)
        );

        outboxEventRepository.save(outboxEvent);
    }
}
