package com.testora.intelligence.impact;

import java.util.List;

/**
 * Flaky detection over recent history (true = passed, oldest first).
 * Rule: a test is flaky when, inside the window, it has BOTH passes and failures and failure rate >= threshold.
 * A test that always fails is broken, not flaky. Window and threshold are configurable by the caller.
 */
public final class FlakinessAnalyzer {
    public record Result(int runs, int failures, double failureRate, boolean flaky, String explanation) { }

    private FlakinessAnalyzer() { }

    public static Result analyze(List<Boolean> passedHistory, int window, double threshold) {
        int from = Math.max(0, passedHistory.size() - window);
        List<Boolean> recent = passedHistory.subList(from, passedHistory.size());
        int failures = (int) recent.stream().filter(p -> !p).count();
        int runs = recent.size();
        double rate = runs == 0 ? 0 : (double) failures / runs;
        boolean mixed = failures > 0 && failures < runs;
        boolean flaky = mixed && rate >= threshold;
        String why = runs == 0 ? "UNKNOWN - no history"
                : failures == runs ? "Consistently failing: broken test or real defect, not flaky"
                : mixed ? String.format("%d/%d failed (%.0f%%) with mixed outcomes; threshold %.0f%%",
                failures, runs, rate * 100, threshold * 100)
                : "No failures in window";
        return new Result(runs, failures, rate, flaky, why);
    }
}
