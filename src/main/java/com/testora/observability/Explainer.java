package com.testora.observability;

import com.testora.core.events.EventBus;
import com.testora.core.events.EventType;
import com.testora.core.events.TestoraEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * "Why mode": answers why TESTORA waited, retried or recovered, from recorded events.
 * Usage: Explainer.why(ctx.executionId()) or Explainer.whyWait(...). Data is dropped at TEST_COMPLETED + explanation flush.
 */
public final class Explainer {
    private static final Map<String, List<TestoraEvent>> EVENTS = new ConcurrentHashMap<>();

    private Explainer() { }

    public static void install() {
        EventBus.global().subscribeAll(e -> {
            if (e.type() == EventType.WAIT_COMPLETED || e.type() == EventType.RETRY_STARTED
                    || e.type() == EventType.RECOVERY_STARTED || e.type() == EventType.RECOVERY_COMPLETED
                    || e.type() == EventType.AI_ANALYSIS_COMPLETED) {
                EVENTS.computeIfAbsent(e.executionId(), k -> new CopyOnWriteArrayList<>()).add(e);
            }
        });
    }

    public static List<String> why(String executionId) {
        List<String> lines = new ArrayList<>();
        for (TestoraEvent e : EVENTS.getOrDefault(executionId, List.of())) {
            Map<String, Object> d = e.data();
            switch (e.type()) {
                case WAIT_COMPLETED -> lines.add("Waited on " + d.get("operation") + " '" + d.get("element") + "': reason="
                        + d.get("waitReason") + ", polling=" + d.get("pollingMs") + "ms, attempts=" + d.get("attempts")
                        + ", duration=" + d.get("durationMs") + "ms, result=" + d.get("outcome"));
                case RETRY_STARTED -> lines.add("Retried " + d.get("operation") + " '" + d.get("element")
                        + "' because " + d.get("reason") + " (attempt " + d.get("attempt") + ")");
                case RECOVERY_STARTED -> lines.add("Recovery started for '" + d.get("element") + "' (original locator "
                        + d.get("original") + " failed)");
                case RECOVERY_COMPLETED -> lines.add("Recovery result for '" + d.get("element") + "': " + d.get("result"));
                default -> lines.add(e.type() + " " + d);
            }
        }
        return lines;
    }

    public static void forget(String executionId) { EVENTS.remove(executionId); }
}
