package com.drf.member.infrastructure.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KafkaTopic {
    MEMBER("member-events");

    private final String name;
}
