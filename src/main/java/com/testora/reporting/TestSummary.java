package com.testora.reporting;

import java.util.List;

/** One row per test execution. Derived from TestContext; contains no secrets. */
public record TestSummary(String testId, String status, long durationMs, int retries, int recoveries,
                          String category, double confidence, String fingerprint, List<String> evidence,
                          List<String> why, String aiAnalysis) { }
