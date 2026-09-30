package com.testora.intelligence.fingerprinting;

import com.testora.core.exceptions.SynchronizationException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/** Deterministic, rule-based failure classification with fingerprinting. Level 2 intelligence: no AI needed. */
public final class FailureClassifier {

    public enum Category {
        APPLICATION_DEFECT, AUTOMATION_DEFECT, ENVIRONMENT_ISSUE, SYNCHRONIZATION_ISSUE,
        TEST_DATA_ISSUE, INFRASTRUCTURE_ISSUE, AUTHENTICATION_ISSUE, UNKNOWN
    }

    /** confidence is the rule's strength, not a probability. */
    public record Classification(Category category, double confidence, String evidence, String fingerprint) { }

    private FailureClassifier() { }

    public static Classification classify(Throwable failure) {
        Throwable root = failure;
        StringBuilder blob = new StringBuilder();
        boolean sync = false;
        boolean network = false;
        boolean noElement = false;
        for (Throwable t = failure; t != null; t = t.getCause()) {
            root = t;
            blob.append(t.getClass().getName()).append(' ').append(String.valueOf(t.getMessage())).append(' ');
            sync |= t instanceof SynchronizationException;
            network |= t instanceof ConnectException || t instanceof UnknownHostException;
            noElement |= t instanceof org.openqa.selenium.NoSuchElementException;
        }
        String text = blob.toString();
        String lower = text.toLowerCase(Locale.ROOT);
        Category category;
        double confidence;
        String evidence;
        if (lower.contains("401") || lower.contains("403") || lower.contains("unauthorized") || lower.contains("forbidden")) {
            category = Category.AUTHENTICATION_ISSUE; confidence = 0.70;
            evidence = "Message contains an authentication/authorization indicator";
        } else if (network || lower.contains("connection refused") || lower.contains("session not created")) {
            category = Category.INFRASTRUCTURE_ISSUE; confidence = 0.80;
            evidence = "Network/session creation failure in cause chain";
        } else if (sync) {
            category = Category.SYNCHRONIZATION_ISSUE; confidence = 0.75;
            evidence = "Wait timed out (see wait diagnostics). May still be an application defect";
        } else if (noElement) {
            category = Category.AUTOMATION_DEFECT; confidence = 0.55;
            evidence = "Element not found outside a managed wait";
        } else if (failure instanceof AssertionError) {
            category = Category.APPLICATION_DEFECT; confidence = 0.50;
            evidence = "Assertion mismatch. Could also be a stale expectation or test-data issue";
        } else {
            category = Category.UNKNOWN; confidence = 0.0;
            evidence = "UNKNOWN - Insufficient Evidence";
        }
        return new Classification(category, confidence, evidence, fingerprint(category, root));
    }

    /** Same category + root exception type + message with volatile parts (digits, ids) removed. */
    static String fingerprint(Category category, Throwable root) {
        String normalized = String.valueOf(root.getMessage()).split("\n")[0]
                .replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F-]{27}", "<id>")
                .replaceAll("\\d+", "<n>");
        String raw = category + "|" + root.getClass().getName() + "|" + normalized;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
