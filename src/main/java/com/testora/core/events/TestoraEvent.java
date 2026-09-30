package com.testora.core.events;

import java.time.Instant;
import java.util.Map;

/** Immutable event. Payload must not contain unmasked secrets. */
public record TestoraEvent(EventType type, String executionId, String correlationId,
                           Instant timestamp, Map<String, Object> data) {

    public static TestoraEvent of(EventType type, String executionId, String correlationId,
                                  Map<String, Object> data) {
        return new TestoraEvent(type, executionId, correlationId, Instant.now(),
                data == null ? Map.of() : Map.copyOf(data));
    }
}
