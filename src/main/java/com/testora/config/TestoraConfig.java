package com.testora.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Loads config/{env}.yaml from the classpath. Precedence: system property > env var > yaml.
 * Secrets are never stored in yaml: use ${ENV_VAR} style keys via {@link #secret(String)}.
 */
public final class TestoraConfig {
    private static final TestoraConfig INSTANCE = new TestoraConfig();

    private final String env = System.getProperty("env", "qa");
    private final Map<String, Object> yaml;

    private TestoraConfig() {
        String path = "config/" + env + ".yaml";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            this.yaml = in == null ? Map.of()
                    : new ObjectMapper(new YAMLFactory()).readValue(in, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read configuration " + path, e);
        }
    }

    public static TestoraConfig get() { return INSTANCE; }

    public String env() { return env; }

    public String string(String key, String fallback) {
        String sys = System.getProperty(key);
        if (sys != null) return sys;
        String envVar = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envVar != null) return envVar;
        Object v = yaml.get(key);
        return v == null ? fallback : v.toString();
    }

    public int integer(String key, int fallback) { return Integer.parseInt(string(key, String.valueOf(fallback))); }

    public double decimal(String key, double fallback) { return Double.parseDouble(string(key, String.valueOf(fallback))); }

    public boolean bool(String key, boolean fallback) { return Boolean.parseBoolean(string(key, String.valueOf(fallback))); }

    /** Secrets come only from environment variables / system properties. */
    public String secret(String name) {
        String v = System.getProperty(name, System.getenv(name));
        if (v == null) throw new IllegalStateException("Missing secret: " + name);
        return v;
    }

    public WaitPolicy webWait() { return policy("web", WaitPolicy.webDefaults()); }

    public WaitPolicy mobileWait() { return policy("mobile", WaitPolicy.mobileDefaults()); }

    public WaitPolicy apiWait() { return policy("api", WaitPolicy.apiDefaults()); }

    private final java.util.concurrent.ConcurrentHashMap<String, WaitPolicy> policies =
            new java.util.concurrent.ConcurrentHashMap<>();

    private WaitPolicy policy(String channel, WaitPolicy defaults) {
        return policies.computeIfAbsent(channel, c -> WaitPolicy.resolve(c, defaults, key -> string(key, null)));
    }
}
