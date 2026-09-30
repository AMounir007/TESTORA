package com.testora.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/** Per-thread LIFO cleanup actions. Always executed, even when the test failed; failures are logged, not thrown. */
public final class CleanupRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupRegistry.class);
    private static final ThreadLocal<Deque<Runnable>> ACTIONS = ThreadLocal.withInitial(ArrayDeque::new);

    private CleanupRegistry() { }

    public static void register(Runnable action) { ACTIONS.get().push(action); }

    public static void runAll() {
        Deque<Runnable> actions = ACTIONS.get();
        while (!actions.isEmpty()) {
            try {
                actions.pop().run();
            } catch (RuntimeException e) {
                LOG.warn("Cleanup action failed: {}", e.toString());
            }
        }
        ACTIONS.remove();
    }
}
