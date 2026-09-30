package com.testora.observability;

import com.testora.core.events.EventBus;
import com.testora.core.events.EventType;
import com.testora.core.events.TestoraEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/** Subscribes to lifecycle events: structured logging + metrics. Core code never calls these directly. */
public final class MetricsListener {
    private static final Logger LOG = LoggerFactory.getLogger("testora.events");

    private MetricsListener() { }

    public static void install() {
        EventBus bus = EventBus.global();
        bus.subscribeAll(e -> LOG.debug("{} [{}] {}", e.type(), e.executionId(), e.data()));
        bus.subscribe(EventType.WAIT_COMPLETED, e -> {
            Metrics.WAITS.increment();
            Metrics.WAIT_MS.add(num(e, "durationMs"));
            Metrics.WAIT_ATTEMPTS.add(num(e, "attempts"));
        });
        bus.subscribe(EventType.RETRY_STARTED, e -> Metrics.RETRIES.increment());
        bus.subscribe(EventType.RECOVERY_COMPLETED, e -> {
            if ("SUCCESS".equals(e.data().get("result"))) Metrics.RECOVERIES.increment();
        });
        bus.subscribe(EventType.TEST_FAILED, e -> Metrics.FAILED.increment());
        bus.subscribe(EventType.TEST_COMPLETED, e -> {
            Metrics.TESTS.increment();
            Metrics.TEST_MS.add(num(e, "durationMs"));
        });
    }

    private static long num(TestoraEvent e, String key) {
        Object v = ((Map<String, Object>) e.data()).get(key);
        try {
            return v == null ? 0 : Long.parseLong(v.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
