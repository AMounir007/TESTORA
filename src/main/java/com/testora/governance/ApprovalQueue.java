package com.testora.governance;

import java.util.Map;

/**
 * Human-in-the-loop queue. Changes to assertions, expected results, business rules or test logic
 * are NEVER applied automatically: they are recorded as PENDING for a human decision.
 */
public final class ApprovalQueue {
    private ApprovalQueue() { }

    public static void request(String change, String evidence, double confidence) {
        AuditTrail.record("APPROVAL_REQUESTED", Map.of(
                "change", change, "evidence", evidence, "confidence", confidence, "status", "PENDING"));
    }
}
