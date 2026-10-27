package com.luxora.commerce.aws.event;

import java.time.Instant;
import java.util.Map;

public record CommerceEvent(
        String eventId,
        String eventType,
        Instant timestamp,
        Map<String, Object> payload
) {
    public static CommerceEvent of(String eventType, Map<String, Object> payload) {
        return new CommerceEvent(
                java.util.UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                payload != null ? payload : Map.of()
        );
    }
}
