package com.testora.api.contracts;

import com.testora.api.contracts.OpenApiSpec.Operation;
import com.testora.config.TestoraConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Endpoint coverage: which "METHOD /path" pairs of the spec were called during this run.
 * ApiClient records every call automatically (query strings are removed). Paths must use the spec template
 * (for example "/customers/{id}" with path parameters), otherwise they cannot be matched.
 * Coverage means "was called", not "was verified well".
 */
public final class ApiCoverage {
    private static final Set<String> CALLED = ConcurrentHashMap.newKeySet();

    private ApiCoverage() { }

    public static void record(String method, String path) {
        int q = path.indexOf('?');
        CALLED.add(method.toUpperCase() + " " + (q >= 0 ? path.substring(0, q) : path));
    }

    public record Coverage(int total, List<String> covered, List<String> missing) {
        public String summary() { return covered.size() + " of " + total + " endpoint/method pairs called"; }
    }

    public static Coverage compute(List<Operation> operations) {
        List<String> covered = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Operation op : operations) (CALLED.contains(op.key()) ? covered : missing).add(op.key());
        return new Coverage(operations.size(), covered, missing);
    }

    /** HTML block for the report. Empty unless the key "openapi.spec" (classpath or file) is configured. */
    public static String html() {
        String location = TestoraConfig.get().string("openapi.spec", "");
        if (location.isBlank()) return "";
        try {
            Coverage c = compute(OpenApiSpec.load(location).operations());
            StringBuilder sb = new StringBuilder("<h2>API contract coverage</h2><p>").append(c.summary()).append("</p>");
            if (!c.missing().isEmpty()) {
                sb.append("<h3>Not called (").append(c.missing().size()).append(")</h3><ul>");
                c.missing().forEach(m -> sb.append("<li>").append(m.replace("&", "&amp;").replace("<", "&lt;")).append("</li>"));
                sb.append("</ul>");
            }
            return sb.toString();
        } catch (RuntimeException e) {
            return "<h2>API contract coverage</h2><p>Unavailable: " + e.getMessage().replace("<", "&lt;") + "</p>";
        }
    }
}
