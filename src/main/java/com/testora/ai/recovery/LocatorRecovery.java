package com.testora.ai.recovery;

import com.testora.config.TestoraConfig;
import com.testora.core.context.TestContext;
import com.testora.core.context.TestContextHolder;
import com.testora.core.events.EventType;
import com.testora.core.events.Events;
import com.testora.governance.AuditTrail;
import com.testora.governance.ApprovalQueue;
import com.testora.governance.ConfidencePolicy;
import com.testora.synchronization.ElementCondition;
import com.testora.synchronization.WaitEngine;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlled locator recovery (proof of concept). Rule-based: tries locators declared by the test author.
 * Disabled by default (recovery.enabled=false). Every attempt is evidence-backed, scored, governed and audited.
 * The original failure is still reported in the audit trail; recovery never silently hides a locator defect.
 * AI-assisted candidates can plug in later by supplying more candidates to this same governed path.
 */
public final class LocatorRecovery {
    private static final double UNIQUE_VISIBLE_FALLBACK_CONFIDENCE = 0.90;

    private LocatorRecovery() { }

    public static Optional<WebElement> recover(WaitEngine engine, String operation, String name, By original,
                                               List<By> fallbacks, ElementCondition... conditions) {
        if (!TestoraConfig.get().bool("recovery.enabled", false) || fallbacks.isEmpty()) return Optional.empty();
        TestContext ctx = TestContextHolder.optional();
        Events.emit(EventType.RECOVERY_STARTED, "operation", operation, "element", name, "original", original);
        ConfidencePolicy policy = ConfidencePolicy.fromConfig();
        for (By candidate : fallbacks) {
            List<WebElement> found = engine.driver().findElements(candidate);
            boolean unique = found.size() == 1;
            boolean visible = unique && found.get(0).isDisplayed();
            if (!visible) continue;
            double confidence = UNIQUE_VISIBLE_FALLBACK_CONFIDENCE;
            ConfidencePolicy.Decision decision = policy.decide(confidence);
            Map<String, Object> audit = Map.of("element", name, "originalLocator", original.toString(),
                    "candidateLocator", candidate.toString(), "confidence", confidence,
                    "evidence", "declared fallback matched exactly one visible element",
                    "decision", decision.name(), "testId", ctx == null ? "n/a" : ctx.testId());
            AuditTrail.record("LOCATOR_RECOVERY", audit);
            if (decision == ConfidencePolicy.Decision.AUTOMATIC) {
                if (ctx != null) ctx.incrementRecoveries();
                Events.emit(EventType.RECOVERY_COMPLETED, "element", name, "result", "SUCCESS",
                        "candidate", candidate, "confidence", confidence);
                return Optional.of(engine.waitForElement(operation, name, candidate, conditions));
            }
            if (decision == ConfidencePolicy.Decision.HUMAN_REVIEW) {
                ApprovalQueue.request("Replace locator " + original + " with " + candidate + " for " + name,
                        "declared fallback matched one visible element", confidence);
            }
            Events.emit(EventType.RECOVERY_COMPLETED, "element", name, "result", decision.name());
            return Optional.empty();
        }
        Events.emit(EventType.RECOVERY_COMPLETED, "element", name, "result", "NO_CANDIDATE");
        return Optional.empty();
    }
}
