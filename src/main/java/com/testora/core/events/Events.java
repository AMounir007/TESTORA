package com.testora.core.events;

import com.testora.core.context.TestContext;
import com.testora.core.context.TestContextHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/** Convenience publisher: Events.emit(TYPE, "key", value, ...). Values are stringified. */
public final class Events {
    private Events() { }

    public static void emit(EventType type, Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            payload.put(String.valueOf(keyValues[i]), String.valueOf(keyValues[i + 1]));
        }
        TestContext ctx = TestContextHolder.optional();
        if (ctx != null) {
            ctx.publish(type, payload);
        } else {
            EventBus.global().publish(TestoraEvent.of(type, "none", "none", payload));
        }
    }
}
