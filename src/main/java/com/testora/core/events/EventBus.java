package com.testora.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Synchronous, thread-safe, in-process event bus.
 * A failing listener never breaks a test: exceptions are logged and isolated.
 */
public final class EventBus {
    private static final Logger LOG = LoggerFactory.getLogger(EventBus.class);
    private static final EventBus INSTANCE = new EventBus();

    private final List<Subscription> subscriptions = new CopyOnWriteArrayList<>();

    private record Subscription(EventType type, Consumer<TestoraEvent> listener) { }

    private EventBus() { }

    public static EventBus global() {
        return INSTANCE;
    }

    /** Subscribe to one event type. */
    public void subscribe(EventType type, Consumer<TestoraEvent> listener) {
        subscriptions.add(new Subscription(type, listener));
    }

    /** Subscribe to all event types. */
    public void subscribeAll(Consumer<TestoraEvent> listener) {
        subscriptions.add(new Subscription(null, listener));
    }

    public void publish(TestoraEvent event) {
        for (Subscription s : subscriptions) {
            if (s.type() == null || s.type() == event.type()) {
                try {
                    s.listener().accept(event);
                } catch (RuntimeException e) {
                    LOG.warn("Event listener failed for {}: {}", event.type(), e.toString());
                }
            }
        }
    }
}
