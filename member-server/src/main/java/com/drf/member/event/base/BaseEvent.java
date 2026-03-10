package com.drf.member.event.base;

import com.github.f4b6a3.tsid.TsidCreator;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public abstract class BaseEvent<T> {
    private final String eventId;
    private final EventType eventType;
    private final LocalDateTime occurredAt;
    private final T payload;

    public BaseEvent(EventType eventType, T payload) {
        this.eventId = TsidCreator.getTsid().toString();
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}


