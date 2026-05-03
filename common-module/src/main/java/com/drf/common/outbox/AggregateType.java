package com.drf.common.outbox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AggregateType {
    MEMBER("member-events");

    final String topic;
}
