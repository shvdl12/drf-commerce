package com.drf.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventTopic {
    MEMBER("member"),
    ORDER("order");

    private final String name;
}