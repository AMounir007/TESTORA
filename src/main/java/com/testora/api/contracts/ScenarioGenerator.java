package com.testora.api.contracts;

import com.testora.api.contracts.OpenApiSpec.Field;
import com.testora.api.contracts.OpenApiSpec.Operation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic scenario generation from the contract. Every scenario names the spec rule it comes from.
 * It checks only what the spec states; business rules still need human-written tests.
 * expected is "2xx" or "4xx" (status class).
 */
public final class ScenarioGenerator {

    public record Scenario(String operation, String name, String specRule, String expected,
                           Map<String, Object> body, Map<String, Object> query, boolean omitAuth) { }

    private ScenarioGenerator() { }

    public static List<Scenario> generate(Operation op) {
        List<Field> bodyFields = op.in("body");
        List<Field> queryFields = op.in("query");
        Map<String, Object> body = new LinkedHashMap<>();
        bodyFields.forEach(f -> body.put(f.name(), sample(f)));
        Map<String, Object> query = new LinkedHashMap<>();
        queryFields.stream().filter(Field::required).forEach(f -> query.put(f.name(), sample(f)));

        List<Scenario> out = new ArrayList<>();
        out.add(new Scenario(op.key(), "valid request", "Spec-conformant request", "2xx", copy(body), copy(query), false));

        for (Field f : bodyFields) {
            if (f.required()) {
                Map<String, Object> b = copy(body);
                b.remove(f.name());
                out.add(body(op, "missing required field '" + f.name() + "'", "required: " + f.name(), "4xx", b, query));
            }
            out.add(body(op, "wrong type for '" + f.name() + "'", "type: " + f.type(), "4xx",
                    with(body, f.name(), wrongType(f)), query));
            if (!f.enumValues().isEmpty()) {
                out.add(body(op, "invalid enum value for '" + f.name() + "'", "enum: " + f.enumValues(), "4xx",
                        with(body, f.name(), "not-in-enum"), query));
            }
            if (f.type().equals("string")) {
                if (f.minLength() != null && f.minLength() > 0) {
                    out.add(body(op, "'" + f.name() + "' below minLength", "minLength: " + f.minLength(), "4xx",
                            with(body, f.name(), "x".repeat(f.minLength() - 1)), query));
                    out.add(body(op, "'" + f.name() + "' at minLength", "minLength: " + f.minLength(), "2xx",
                            with(body, f.name(), "x".repeat(f.minLength())), query));
                }
                if (f.maxLength() != null) {
                    out.add(body(op, "'" + f.name() + "' at maxLength", "maxLength: " + f.maxLength(), "2xx",
                            with(body, f.name(), "x".repeat(f.maxLength())), query));
                    out.add(body(op, "'" + f.name() + "' above maxLength", "maxLength: " + f.maxLength(), "4xx",
                            with(body, f.name(), "x".repeat(f.maxLength() + 1)), query));
                }
            }
            if (isNumber(f)) {
                if (f.minimum() != null) {
                    out.add(body(op, "'" + f.name() + "' below minimum", "minimum: " + f.minimum(), "4xx",
                            with(body, f.name(), num(f, f.minimum() - 1)), query));
                    out.add(body(op, "'" + f.name() + "' at minimum", "minimum: " + f.minimum(), "2xx",
                            with(body, f.name(), num(f, f.minimum())), query));
                }
                if (f.maximum() != null) {
                    out.add(body(op, "'" + f.name() + "' above maximum", "maximum: " + f.maximum(), "4xx",
                            with(body, f.name(), num(f, f.maximum() + 1)), query));
                    out.add(body(op, "'" + f.name() + "' at maximum", "maximum: " + f.maximum(), "2xx",
                            with(body, f.name(), num(f, f.maximum())), query));
                }
            }
        }
        for (Field f : queryFields) {
            if (f.required()) {
                Map<String, Object> q = copy(query);
                q.remove(f.name());
                out.add(new Scenario(op.key(), "missing required query parameter '" + f.name() + "'",
                        "required query: " + f.name(), "4xx", copy(body), q, false));
            }
        }
        if (op.secured()) {
            out.add(new Scenario(op.key(), "no credentials", "security is declared", "4xx", copy(body), copy(query), true));
        }
        return out;
    }

    private static Scenario body(Operation op, String name, String rule, String expected,
                                 Map<String, Object> body, Map<String, Object> query) {
        return new Scenario(op.key(), name, rule, expected, body, copy(query), false);
    }

    private static boolean isNumber(Field f) { return f.type().equals("integer") || f.type().equals("number"); }

    private static Object num(Field f, double value) {
        return f.type().equals("integer") ? (Object) (long) value : (Object) value;
    }

    static Object sample(Field f) {
        if (!f.enumValues().isEmpty()) return f.enumValues().get(0);
        return switch (f.type()) {
            case "integer" -> f.minimum() != null ? (Object) (long) Math.ceil(f.minimum()) : (Object) 1L;
            case "number" -> f.minimum() != null ? (Object) f.minimum() : (Object) 1.5;
            case "boolean" -> true;
            case "array" -> List.of();
            case "object" -> Map.of();
            default -> "x".repeat(Math.max(f.minLength() == null ? 1 : f.minLength(), 1));
        };
    }

    private static Object wrongType(Field f) { return f.type().equals("string") ? (Object) 12345 : "not-a-" + f.type(); }

    private static Map<String, Object> copy(Map<String, Object> m) { return new LinkedHashMap<>(m); }

    private static Map<String, Object> with(Map<String, Object> m, String key, Object value) {
        Map<String, Object> c = copy(m);
        c.put(key, value);
        return c;
    }
}
