package com.testora.ai.diagnosis;

import com.testora.ai.gateway.AiGateway;
import com.testora.ai.gateway.AiRequest;
import com.testora.ai.gateway.AiResponse;
import com.testora.config.TestoraConfig;
import com.testora.core.events.EventType;
import com.testora.core.events.Events;
import com.testora.intelligence.fingerprinting.FailureClassifier;
import com.testora.intelligence.fingerprinting.FailureClassifier.Classification;

import java.util.Optional;

/**
 * Deterministic classification first. AI is consulted only when rule confidence is below a configurable threshold,
 * and its answer is labelled as inference, never as fact. Without AI the result is still complete.
 */
public final class FailureAnalyzer {
    public record Analysis(Classification classification, Optional<String> aiInference) { }

    private FailureAnalyzer() { }

    public static Analysis analyze(Throwable failure, AiGateway gateway) {
        Classification rule = FailureClassifier.classify(failure);
        double threshold = TestoraConfig.get().decimal("ai.analysis.threshold", 0.80);
        if (rule.confidence() >= threshold || !gateway.enabled()) {
            return new Analysis(rule, Optional.empty());
        }
        Events.emit(EventType.AI_ANALYSIS_STARTED, "category", rule.category());
        String prompt = """
                You analyse a failed automated test. Use ONLY the evidence below.
                Label every statement FACT, INFERENCE, RECOMMENDATION or UNKNOWN.
                If evidence is insufficient answer exactly: UNKNOWN - Insufficient Evidence.
                Never invent logs, DOM, API responses or statistics.

                Rule-based classification: %s (rule confidence %.2f) because: %s
                Exception: %s
                Message: %s
                """.formatted(rule.category(), rule.confidence(), rule.evidence(),
                failure.getClass().getName(), failure.getMessage());
        Optional<String> text = gateway.ask(new AiRequest("failure-analysis", prompt, 500))
                .map(AiResponse::text).map(t -> "[AI-INFERENCE, not verified] " + t);
        Events.emit(EventType.AI_ANALYSIS_COMPLETED, "answered", text.isPresent());
        return new Analysis(rule, text);
    }
}
