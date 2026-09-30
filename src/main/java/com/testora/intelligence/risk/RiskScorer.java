package com.testora.intelligence.risk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transparent risk model: risk = sum(weight_i * factor_i), every factor normalized to 0..1, weights sum to 1.
 * The score only ORDERS tests; it is not a quality metric. Weights are explicit and meant to be tuned per
 * organization with real defect history. The breakdown is returned so every rank is explainable.
 */
public final class RiskScorer {
    public record Input(double businessCriticality, double customerImpact, double changeFrequency,
                        double historicalDefects, double recentFailureRate, double integrationComplexity,
                        double instability) { }

    public record Risk(double score, Map<String, Double> contributions) { }

    private static final Map<String, Double> WEIGHTS = Map.of(
            "businessCriticality", 0.25, "customerImpact", 0.20, "changeFrequency", 0.10,
            "historicalDefects", 0.15, "recentFailureRate", 0.15, "integrationComplexity", 0.10,
            "instability", 0.05);

    private RiskScorer() { }

    public static Risk score(Input in) {
        Map<String, Double> factors = Map.of(
                "businessCriticality", in.businessCriticality(), "customerImpact", in.customerImpact(),
                "changeFrequency", in.changeFrequency(), "historicalDefects", in.historicalDefects(),
                "recentFailureRate", in.recentFailureRate(), "integrationComplexity", in.integrationComplexity(),
                "instability", in.instability());
        Map<String, Double> contributions = new LinkedHashMap<>();
        double total = 0;
        for (var e : WEIGHTS.entrySet()) {
            double c = e.getValue() * Math.max(0, Math.min(1, factors.get(e.getKey())));
            contributions.put(e.getKey(), c);
            total += c;
        }
        return new Risk(total, contributions);
    }
}
