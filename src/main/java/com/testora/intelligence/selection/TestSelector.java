package com.testora.intelligence.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Explainable selection: impacted tests first, then highest risk, until the budget is used. Budget stays configurable. */
public final class TestSelector {
    public record Candidate(String testId, double risk, double instability) { }

    public record Selected(String testId, String reason) { }

    private TestSelector() { }

    public static List<Selected> select(List<Candidate> candidates, Set<String> impacted, int budget) {
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparing((Candidate c) -> !impacted.contains(c.testId()))
                .thenComparing(Comparator.comparingDouble(Candidate::risk).reversed()));
        List<Selected> plan = new ArrayList<>();
        for (Candidate c : sorted) {
            if (plan.size() >= budget) break;
            String reason = impacted.contains(c.testId())
                    ? "Impacted by change (risk " + String.format("%.2f", c.risk()) + ")"
                    : "Top risk (" + String.format("%.2f", c.risk()) + ")";
            if (c.instability() > 0.3) reason += "; note: unstable test, failures need extra scrutiny";
            plan.add(new Selected(c.testId(), reason));
        }
        return plan;
    }
}
