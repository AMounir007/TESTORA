package com.testora.synchronization;

import com.testora.config.WaitPolicy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Records what a wait observed so failures are actionable and explainable ("Why mode"). */
public final class WaitDiagnostics {
    private final String operation;
    private final String element;
    private final String locator;
    private final WaitPolicy policy;
    private final Map<String, Boolean> conditions = new LinkedHashMap<>();
    private int attempts;
    private String lastState = "No attempt made";
    private String firstBlocker = "none";

    public WaitDiagnostics(String operation, String element, String locator, WaitPolicy policy) {
        this.operation = operation;
        this.element = element;
        this.locator = locator;
        this.policy = policy;
    }

    void nextAttempt() { attempts++; }

    void condition(String name, boolean value) { conditions.put(name, value); }

    void state(String state) {
        lastState = state;
        if (firstBlocker.equals("none") && !state.equals("Conditions satisfied")) firstBlocker = state;
    }

    public int attempts() { return attempts; }
    public String lastState() { return lastState; }
    public String firstBlocker() { return firstBlocker; }

    public String report() {
        StringBuilder sb = new StringBuilder("Synchronization Timeout\n");
        sb.append("  Operation: ").append(operation).append('\n');
        sb.append("  Element: ").append(element).append('\n');
        sb.append("  Locator: ").append(locator).append('\n');
        sb.append("  Timeout: ").append(policy.timeout().toSeconds()).append(" seconds\n");
        sb.append("  Polling: ").append(policy.pollingInterval().toMillis()).append(" ms\n");
        sb.append("  Conditions:\n");
        conditions.forEach((k, v) -> sb.append("    ").append(k).append(" = ").append(v ? "YES" : "NO").append('\n'));
        sb.append("  Attempts: ").append(attempts).append('\n');
        sb.append("  Last State: ").append(lastState).append('\n');
        sb.append("  Possible Causes: ").append(causes()).append('\n');
        return sb.toString();
    }

    private String causes() {
        if (Boolean.FALSE.equals(conditions.get("Present"))) return "Locator issue, page not loaded, application state";
        if (conditions.containsValue(Boolean.FALSE)) return "Overlay, animation, application state, disabled control";
        return "Unknown (insufficient evidence)";
    }
}
