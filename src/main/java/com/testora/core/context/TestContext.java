package com.testora.core.context;

import com.testora.core.events.EventBus;
import com.testora.core.events.EventType;
import com.testora.core.events.TestoraEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-execution, thread-safe state shared by Web, API and Mobile channels.
 * Bound to the current thread via {@link TestContextHolder}.
 */
public final class TestContext {
    private final String executionId = UUID.randomUUID().toString();
    private final String correlationId = UUID.randomUUID().toString();
    private final String testId;
    private final String environment;
    private final Instant startTime = Instant.now();
    private volatile Instant endTime;

    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final List<String> actions = new CopyOnWriteArrayList<>();
    private final List<String> evidence = new CopyOnWriteArrayList<>();
    private final AtomicInteger retries = new AtomicInteger();
    private final AtomicInteger recoveries = new AtomicInteger();
    private volatile String aiAnalysis;

    public TestContext(String testId, String environment) {
        this.testId = testId;
        this.environment = environment;
    }

    public String executionId() { return executionId; }
    public String correlationId() { return correlationId; }
    public String testId() { return testId; }
    public String environment() { return environment; }
    public Instant startTime() { return startTime; }
    public Optional<Instant> endTime() { return Optional.ofNullable(endTime); }

    public void put(String key, Object value) { data.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) { return Optional.ofNullable((T) data.get(key)); }

    public void recordAction(String action) { actions.add(action); }
    public List<String> actions() { return List.copyOf(actions); }
    public void addEvidence(String reference) { evidence.add(reference); }
    public List<String> evidence() { return List.copyOf(evidence); }
    public int incrementRetries() { return retries.incrementAndGet(); }
    public int retries() { return retries.get(); }
    public int incrementRecoveries() { return recoveries.incrementAndGet(); }
    public int recoveries() { return recoveries.get(); }
    public Optional<String> aiAnalysis() { return Optional.ofNullable(aiAnalysis); }
    public void aiAnalysis(String result) { this.aiAnalysis = result; }
    public void complete() { this.endTime = Instant.now(); }

    public void publish(EventType type, Map<String, Object> payload) {
        EventBus.global().publish(TestoraEvent.of(type, executionId, correlationId, payload));
    }
}
