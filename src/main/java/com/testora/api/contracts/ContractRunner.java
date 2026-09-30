package com.testora.api.contracts;

import com.testora.api.clients.ApiClient;
import com.testora.api.contracts.OpenApiSpec.Field;
import com.testora.api.contracts.OpenApiSpec.Operation;
import com.testora.api.contracts.ScenarioGenerator.Scenario;
import io.restassured.response.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/** Executes a generated scenario through ApiClient and compares the status CLASS with the expectation. */
public final class ContractRunner {

    public record Result(String operation, String scenario, String specRule, String expected, int status,
                         boolean passed, boolean skipped) {
        @Override public String toString() {
            return (skipped ? "SKIPPED " : passed ? "PASS " : "FAIL ") + operation + " | " + scenario
                    + " | rule: " + specRule + " | expected " + expected + ", got " + (skipped ? "-" : status);
        }
    }

    private ContractRunner() { }

    /** anonymous may be null: then "no credentials" scenarios are skipped (and reported as skipped). */
    public static Result run(ApiClient authed, ApiClient anonymous, Operation op, Scenario s) {
        ApiClient client = s.omitAuth() ? anonymous : authed;
        if (client == null) return new Result(s.operation(), s.name(), s.specRule(), s.expected(), -1, true, true);

        Object[] pathParams = op.in("path").stream().map(ContractRunner::pathValue).toArray();
        Object body = op.in("body").isEmpty() ? null : s.body();
        String path = op.path();
        Response r;
        switch (op.method()) {
            case "GET" -> r = client.get(path, s.query(), pathParams);
            case "DELETE" -> r = client.delete(withQuery(path, s), pathParams);
            case "POST" -> r = client.post(withQuery(path, s), body, pathParams);
            case "PUT" -> r = client.put(withQuery(path, s), body, pathParams);
            case "PATCH" -> r = client.patch(withQuery(path, s), body, pathParams);
            default -> throw new IllegalArgumentException("Unsupported method " + op.method());
        }
        int status = r.statusCode();
        boolean ok = status / 100 == (s.expected().equals("2xx") ? 2 : 4);
        return new Result(s.operation(), s.name(), s.specRule(), s.expected(), status, ok, false);
    }

    /** Runs every generated scenario of every operation. */
    public static List<Result> runAll(ApiClient authed, ApiClient anonymous, List<Operation> operations) {
        return operations.stream()
                .flatMap(op -> ScenarioGenerator.generate(op).stream().map(s -> run(authed, anonymous, op, s)))
                .collect(Collectors.toList());
    }

    private static Object pathValue(Field f) { return ScenarioGenerator.sample(f); }

    private static String withQuery(String path, Scenario s) {
        if (s.query().isEmpty()) return path;
        String q = s.query().entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return path + "?" + q;
    }
}
