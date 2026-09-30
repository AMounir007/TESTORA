package com.testora.execution.history;

import com.testora.config.TestoraConfig;
import com.testora.intelligence.impact.FlakinessAnalyzer;
import com.testora.intelligence.selection.TestSelector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns history into triage information. All rules are deterministic and configurable:
 *  history.window (default 20 runs), history.flaky.threshold (0.10), history.min.runs (5).
 * "Flaky" = mixed pass/fail in the window. "Broken" = failed every time in the window (min 3 runs).
 * Numbers describe past runs only; they do not prove a cause.
 */
public final class TrendAnalyzer {

    public record Trends(List<String> flaky, List<String> broken, List<String> passedWithHelp,
                         Set<String> newFingerprints, Set<String> recurringFingerprints, int historyRuns) { }

    private TrendAnalyzer() { }

    public static Trends compute() { return compute(HistoryStore.readAll(), HistoryStore.RUN_ID); }

    public static Trends compute(List<HistoryRecord> all, String currentRunId) {
        TestoraConfig cfg = TestoraConfig.get();
        int window = cfg.integer("history.window", 20);
        double threshold = cfg.decimal("history.flaky.threshold", 0.10);
        int minRuns = cfg.integer("history.min.runs", 5);

        Map<String, List<HistoryRecord>> byTest = all.stream()
                .collect(Collectors.groupingBy(HistoryRecord::testId, LinkedHashMap::new, Collectors.toList()));
        List<String> flaky = new ArrayList<>();
        List<String> broken = new ArrayList<>();
        byTest.forEach((test, records) -> {
            FlakinessAnalyzer.Result r = FlakinessAnalyzer.analyze(
                    records.stream().map(HistoryRecord::passed).toList(), window, threshold);
            if (r.flaky() && r.runs() >= minRuns) flaky.add(test + " - " + r.explanation());
            if (r.runs() >= 3 && r.failures() == r.runs()) broken.add(test + " - failed " + r.runs() + "/" + r.runs());
        });

        List<HistoryRecord> current = all.stream().filter(h -> h.runId().equals(currentRunId)).toList();
        List<HistoryRecord> earlier = all.stream().filter(h -> !h.runId().equals(currentRunId)).toList();
        Set<String> earlierFingerprints = earlier.stream().filter(h -> !h.passed())
                .map(HistoryRecord::fingerprint).collect(Collectors.toSet());

        List<String> passedWithHelp = current.stream().filter(h -> h.passed() && (h.retries() > 0 || h.recoveries() > 0))
                .map(h -> h.testId() + " passed after " + h.retries() + " retry(ies), " + h.recoveries() + " recovery(ies)")
                .toList();
        Set<String> fresh = new LinkedHashSet<>();
        Set<String> recurring = new LinkedHashSet<>();
        current.stream().filter(h -> !h.passed()).forEach(h ->
                (earlierFingerprints.contains(h.fingerprint()) ? recurring : fresh).add(h.fingerprint()));
        int runs = (int) all.stream().map(HistoryRecord::runId).distinct().count();
        return new Trends(flaky, broken, passedWithHelp, fresh, recurring, runs);
    }

    /** Ranked run list: tests with the highest recent failure rate first (explainable, history-based only). */
    public static List<TestSelector.Selected> rankByRecentFailures(Set<String> impacted, int budget) {
        int window = TestoraConfig.get().integer("history.window", 20);
        Map<String, List<HistoryRecord>> byTest = HistoryStore.readAll().stream()
                .collect(Collectors.groupingBy(HistoryRecord::testId, LinkedHashMap::new, Collectors.toList()));
        List<TestSelector.Candidate> candidates = new ArrayList<>();
        byTest.forEach((test, records) -> {
            List<HistoryRecord> recent = records.subList(Math.max(0, records.size() - window), records.size());
            long failures = recent.stream().filter(h -> !h.passed()).count();
            double rate = recent.isEmpty() ? 0 : (double) failures / recent.size();
            boolean mixed = failures > 0 && failures < recent.size();
            candidates.add(new TestSelector.Candidate(test, rate, mixed ? rate : 0));
        });
        return TestSelector.select(candidates, impacted, budget);
    }

    public static String html() { return html(compute()); }

    static String html(Trends t) {
        StringBuilder sb = new StringBuilder("<h2>Trends</h2><p>History: ").append(t.historyRuns())
                .append(" run(s). Based on past results only; a pattern is not proof of cause.</p>");
        section(sb, "Flaky tests (mixed results)", t.flaky());
        section(sb, "Broken tests (always failing)", t.broken());
        section(sb, "Passed only with retries/recovery (review these)", t.passedWithHelp());
        section(sb, "New failure fingerprints", List.copyOf(t.newFingerprints()));
        section(sb, "Recurring failure fingerprints", List.copyOf(t.recurringFingerprints()));
        return sb.toString();
    }

    private static void section(StringBuilder sb, String title, List<String> items) {
        sb.append("<h3>").append(title).append(" (").append(items.size()).append(")</h3>");
        if (items.isEmpty()) return;
        sb.append("<ul>");
        items.forEach(i -> sb.append("<li>").append(i.replace("&", "&amp;").replace("<", "&lt;")).append("</li>"));
        sb.append("</ul>");
    }
}
