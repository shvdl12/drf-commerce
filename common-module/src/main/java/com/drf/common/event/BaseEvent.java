package com.drf.common.event;

import com.github.f4b6a3.tsid.TsidCreator;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
public abstract class BaseEvent<T> {
    private final Long eventId;
    private final String eventType;
    private final LocalDateTime occurredAt;
    private final T payload;

    public BaseEvent(String eventType, T payload) {
        this.eventId = TsidCreator.getTsid().toLong();
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
