package com.testora.config;

import java.time.Duration;

/** Centralized synchronization settings. One policy per channel (web/api/mobile). */
public record WaitPolicy(Duration timeout, Duration pollingInterval, Duration pageLoadTimeout,
                         Duration scriptTimeout, boolean ignoreStale, boolean ignoreNoSuchElement,
                         int maxStaleRecoveries) {

    public static WaitPolicy webDefaults() {
        return new WaitPolicy(Duration.ofSeconds(15), Duration.ofMillis(300),
                Duration.ofSeconds(30), Duration.ofSeconds(30), true, true, 3);
    }
}
