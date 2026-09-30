package com.testora.synchronization;

import com.testora.config.TestoraConfig;

/**
 * Action retry policy (re-running one interaction). Independent from:
 *  - synchronization retry: handled by WaitEngine polling within WaitPolicy.timeout
 *  - test retry: surefire rerunFailingTestsCount, reported separately
 * Retries are always bounded and always emitted as RETRY_STARTED events so they cannot hide defects.
 */
public record RetryPolicy(int maxAttempts) {
    public static RetryPolicy action() {
        return new RetryPolicy(Math.max(1, TestoraConfig.get().integer("retry.action.max", 2)));
    }
}
