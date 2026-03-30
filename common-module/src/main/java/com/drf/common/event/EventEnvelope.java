package com.drf.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(
        @JsonProperty(required = true) long eventId,
        @JsonProperty(required = true) String eventType,
        @JsonProperty(required = true) JsonNode payload
) {
}
