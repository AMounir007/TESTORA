package com.testora.observability;

import java.util.concurrent.atomic.LongAdder;

/** In-memory framework telemetry. Fed by events (see MetricsListener) and a few direct probes. */
public final class Metrics {
    static final LongAdder TESTS = new LongAdder();
    static final LongAdder FAILED = new LongAdder();
    static final LongAdder TEST_MS = new LongAdder();
    static final LongAdder WAITS = new LongAdder();
    static final LongAdder WAIT_MS = new LongAdder();
    static final LongAdder WAIT_ATTEMPTS = new LongAdder();
    static final LongAdder RETRIES = new LongAdder();
    static final LongAdder RECOVERIES = new LongAdder();
    static final LongAdder DRIVERS = new LongAdder();
    static final LongAdder DRIVER_MS = new LongAdder();
    static final LongAdder API_CALLS = new LongAdder();
    static final LongAdder API_MS = new LongAdder();
    static final LongAdder AI_CALLS = new LongAdder();
    static final LongAdder AI_MS = new LongAdder();

    private Metrics() { }

    public static void recordDriverCreation(long ms) { DRIVERS.increment(); DRIVER_MS.add(ms); }

    public static void recordApi(long ms) { API_CALLS.increment(); API_MS.add(ms); }

    public static void recordAi(long ms) { AI_CALLS.increment(); AI_MS.add(ms); }

    private static String avg(LongAdder sum, LongAdder count) {
        return count.sum() == 0 ? "n/a" : (sum.sum() / count.sum()) + " ms";
    }

    /** Wait overhead = share of test time spent waiting. A measurement, not a performance claim. */
    public static String summary() {
        double waitShare = TEST_MS.sum() == 0 ? 0 : 100.0 * WAIT_MS.sum() / TEST_MS.sum();
        double retryRate = TESTS.sum() == 0 ? 0 : 100.0 * RETRIES.sum() / TESTS.sum();
        return String.format("TESTORA telemetry: tests=%d failed=%d avgTest=%s avgWait=%s avgWaitAttempts=%s "
                        + "waitShareOfTestTime=%.1f%% retriesPerTest=%.1f%% recoveries=%d avgDriverCreation=%s "
                        + "avgApi=%s aiCalls=%d avgAi=%s",
                TESTS.sum(), FAILED.sum(), avg(TEST_MS, TESTS), avg(WAIT_MS, WAITS),
                WAITS.sum() == 0 ? "n/a" : String.valueOf(WAIT_ATTEMPTS.sum() / WAITS.sum()),
                waitShare, retryRate, RECOVERIES.sum(), avg(DRIVER_MS, DRIVERS), avg(API_MS, API_CALLS),
                AI_CALLS.sum(), avg(AI_MS, AI_CALLS));
    }
}
