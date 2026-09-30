package com.testora.governance;

import com.testora.core.utilities.Jsonl;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Append-only audit of every intelligent action: action + evidence + confidence + policy + decision. */
public final class AuditTrail {
    private AuditTrail() { }

    public static void record(String action, Map<String, Object> fields) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("action", action);
        entry.putAll(fields);
        Jsonl.append(Jsonl.outputDir().resolve("audit.jsonl"), entry);
    }
}
