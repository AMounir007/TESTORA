package com.testora.governance;

import com.testora.config.TestoraConfig;

/** Configurable confidence policy. No universal thresholds: defaults are overridable per environment. */
public record ConfidencePolicy(double autoThreshold, double reviewThreshold) {

    public enum Decision { AUTOMATIC, HUMAN_REVIEW, RECOMMEND_ONLY }

    public static ConfidencePolicy fromConfig() {
        TestoraConfig c = TestoraConfig.get();
        return new ConfidencePolicy(c.decimal("governance.auto.threshold", 0.85),
                c.decimal("governance.review.threshold", 0.60));
    }

    public Decision decide(double confidence) {
        if (confidence >= autoThreshold) return Decision.AUTOMATIC;
        if (confidence >= reviewThreshold) return Decision.HUMAN_REVIEW;
        return Decision.RECOMMEND_ONLY;
    }
}
