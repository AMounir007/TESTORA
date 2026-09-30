package com.testora.execution.history;

/** One persisted test result. Contains no secrets or test data. */
public record HistoryRecord(String runId, String testId, String status, String timestamp, long durationMs,
                            int retries, int recoveries, String category, String fingerprint) {
    public boolean passed() { return "PASSED".equals(status); }
}
