package com.testora.api.contracts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.testora.core.exceptions.TestoraException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAPI 3.x reader (JSON or YAML). Understands endpoints, path/query parameters, JSON request-body
 * fields (type, required, length, range, enum), security and response codes. Resolves local "#/..." $refs.
 * Not supported yet: Swagger 2, remote $refs, oneOf/anyOf/allOf, nested object validation.
 */
public final class OpenApiSpec {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete");

    /** in = "path", "query" or "body". */
    public record Field(String name, String in, String type, boolean required, Integer minLength, Integer maxLength,
                        Double minimum, Double maximum, List<Object> enumValues) { }

    public record Operation(String method, String path, List<Field> fields, boolean secured, List<String> responseCodes) {
        public String key() { return method + " " + path; }

        public List<Field> in(String location) { return fields.stream().filter(f -> f.in().equals(location)).toList(); }
    }

    private final JsonNode root;
    private final List<Operation> operations = new ArrayList<>();

    private OpenApiSpec(JsonNode root) {
        this.root = root;
        parse();
    }

    public static OpenApiSpec parse(String content) {
        try {
            JsonNode node = MAPPER.readTree(content);
            if (node == null || !node.has("paths")) throw new TestoraException("Not an OpenAPI 3 document: 'paths' is missing");
            return new OpenApiSpec(node);
        } catch (IOException e) {
            throw new TestoraException("Cannot parse OpenAPI document: " + e.getMessage(), e);
        }
    }

    /** Loads from the classpath first, then from a file path. */
    public static OpenApiSpec load(String location) {
        try (InputStream in = OpenApiSpec.class.getClassLoader().getResourceAsStream(location)) {
            if (in != null) return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            return parse(Files.readString(Path.of(location), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new TestoraException("Cannot read OpenAPI file '" + location + "'", e);
        }
    }

    public List<Operation> operations() { return List.copyOf(operations); }

    private void parse() {
        boolean globalSecurity = root.path("security").isArray() && !root.path("security").isEmpty();
        Iterator<Map.Entry<String, JsonNode>> paths = root.path("paths").fields();
        while (paths.hasNext()) {
            var pathEntry = paths.next();
            JsonNode pathItem = pathEntry.getValue();
            for (String method : METHODS) {
                JsonNode op = pathItem.path(method);
                if (op.isMissingNode()) continue;
                List<Field> fields = new ArrayList<>();
                readParameters(pathItem.path("parameters"), fields);
                readParameters(op.path("parameters"), fields);
                readBody(op.path("requestBody"), fields);
                boolean secured = op.has("security") ? !op.path("security").isEmpty() : globalSecurity;
                List<String> codes = new ArrayList<>();
                op.path("responses").fieldNames().forEachRemaining(codes::add);
                operations.add(new Operation(method.toUpperCase(), pathEntry.getKey(), fields, secured, codes));
            }
        }
    }

    private void readParameters(JsonNode params, List<Field> out) {
        for (JsonNode raw : params) {
            JsonNode p = resolve(raw);
            String in = p.path("in").asText();
            if (!in.equals("path") && !in.equals("query")) continue;
            boolean required = in.equals("path") || p.path("required").asBoolean(false);
            out.add(field(p.path("name").asText(), in, required, resolve(p.path("schema"))));
        }
    }

    private void readBody(JsonNode requestBody, List<Field> out) {
        JsonNode schema = resolve(resolve(requestBody).path("content").path("application/json").path("schema"));
        if (schema.isMissingNode()) return;
        List<String> required = new ArrayList<>();
        schema.path("required").forEach(n -> required.add(n.asText()));
        Iterator<Map.Entry<String, JsonNode>> props = schema.path("properties").fields();
        while (props.hasNext()) {
            var e = props.next();
            out.add(field(e.getKey(), "body", required.contains(e.getKey()), resolve(e.getValue())));
        }
    }

    private static Field field(String name, String in, boolean required, JsonNode schema) {
        List<Object> enums = new ArrayList<>();
        schema.path("enum").forEach(n -> enums.add(n.isNumber() ? n.numberValue() : n.asText()));
        return new Field(name, in, schema.path("type").asText("string"), required,
                schema.has("minLength") ? schema.get("minLength").asInt() : null,
                schema.has("maxLength") ? schema.get("maxLength").asInt() : null,
                schema.has("minimum") ? schema.get("minimum").asDouble() : null,
                schema.has("maximum") ? schema.get("maximum").asDouble() : null, enums);
    }

    private JsonNode resolve(JsonNode node) {
        JsonNode current = node;
        for (int depth = 0; depth < 10 && current.has("$ref"); depth++) {
            String ref = current.get("$ref").asText();
            if (!ref.startsWith("#/")) return current;
            current = root.at(ref.substring(1));
        }
        return current;
    }
}
