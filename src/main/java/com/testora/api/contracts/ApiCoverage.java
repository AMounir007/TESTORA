package com.testora.api.contracts;
}
    }
        }
            return "<h2>API contract coverage</h2><p>Unavailable: " + e.getMessage().replace("<", "&lt;") + "</p>";
        } catch (RuntimeException e) {
            return sb.toString();
            }
                sb.append("</ul>");
                c.missing().forEach(m -> sb.append("<li>").append(m.replace("&", "&amp;").replace("<", "&lt;")).append("</li>"));
                sb.append("<h3>Not called (").append(c.missing().size()).append(")</h3><ul>");
            if (!c.missing().isEmpty()) {
            StringBuilder sb = new StringBuilder("<h2>API contract coverage</h2><p>").append(c.summary()).append("</p>");
            Coverage c = compute(OpenApiSpec.load(location).operations());
        try {
        if (location.isBlank()) return "";
        String location = TestoraConfig.get().string("openapi.spec", "");
    public static String html() {
    /** HTML block for the report. Empty unless the key "openapi.spec" (classpath or file) is configured. */

    }
        return new Coverage(operations.size(), covered, missing);
        for (Operation op : operations) (CALLED.contains(op.key()) ? covered : missing).add(op.key());
        List<String> missing = new ArrayList<>();
        List<String> covered = new ArrayList<>();
    public static Coverage compute(List<Operation> operations) {

    }
        public String summary() { return covered.size() + " of " + total + " endpoint/method pairs called"; }
    public record Coverage(int total, List<String> covered, List<String> missing) {

    }
        CALLED.add(method.toUpperCase() + " " + (q >= 0 ? path.substring(0, q) : path));
        int q = path.indexOf('?');
    public static void record(String method, String path) {

    private ApiCoverage() { }

    private static final Set<String> CALLED = ConcurrentHashMap.newKeySet();
public final class ApiCoverage {
 */
 * Coverage means "was called", not "was verified well".
 * (for example "/customers/{id}" with path parameters), otherwise they cannot be matched.
 * ApiClient records every call automatically (query strings are removed). Paths must use the spec template
 * Endpoint coverage: which "METHOD /path" pairs of the spec were called during this run.
/**

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import com.testora.api.contracts.OpenApiSpec.Operation;
import com.testora.config.TestoraConfig;

