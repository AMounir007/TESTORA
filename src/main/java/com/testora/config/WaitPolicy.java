package com.testora.config;

import com.testora.core.exceptions.TestoraException;

import java.time.Duration;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized synchronization settings, one policy per channel (web, mobile, api).
 * Keys: wait.CHANNEL.timeout, .polling, .pageLoadTimeout, .scriptTimeout, .ignoreStale,
 * .ignoreNoSuchElement, .maxStaleRecoveries. Durations: "300ms", "15s", "2m" (a plain number means seconds).
 * For api, "timeout" is the connection and response timeout of each HTTP call.
 */
public record WaitPolicy(Duration timeout, Duration pollingInterval, Duration pageLoadTimeout,
                         Duration scriptTimeout, boolean ignoreStale, boolean ignoreNoSuchElement,
                         int maxStaleRecoveries) {

    private static final Pattern DURATION = Pattern.compile("^(\\d+)\\s*(ms|s|m)?$");

    public static WaitPolicy webDefaults() {
        return new WaitPolicy(Duration.ofSeconds(15), Duration.ofMillis(300),
                Duration.ofSeconds(30), Duration.ofSeconds(30), true, true, 3);
    }

    /** Real devices and Appium round trips are slower than a local browser. */
    public static WaitPolicy mobileDefaults() {
        return new WaitPolicy(Duration.ofSeconds(30), Duration.ofMillis(500),
                Duration.ofSeconds(60), Duration.ofSeconds(60), true, true, 3);
    }

    public static WaitPolicy apiDefaults() {
        return new WaitPolicy(Duration.ofSeconds(30), Duration.ofMillis(500),
                Duration.ofSeconds(30), Duration.ofSeconds(30), false, false, 0);
    }

    /** Builds the policy for a channel: every key falls back to the given defaults. Fails fast on bad values. */
    public static WaitPolicy resolve(String channel, WaitPolicy defaults, Function<String, String> lookup) {
        String prefix = "wait." + channel + ".";
        Duration timeout = duration(prefix + "timeout", lookup, defaults.timeout());
        Duration polling = duration(prefix + "polling", lookup, defaults.pollingInterval());
        Duration pageLoad = duration(prefix + "pageLoadTimeout", lookup, defaults.pageLoadTimeout());
        Duration script = duration(prefix + "scriptTimeout", lookup, defaults.scriptTimeout());
        boolean stale = bool(prefix + "ignoreStale", lookup, defaults.ignoreStale());
        boolean noSuch = bool(prefix + "ignoreNoSuchElement", lookup, defaults.ignoreNoSuchElement());
        int maxStale = integer(prefix + "maxStaleRecoveries", lookup, defaults.maxStaleRecoveries());

        if (timeout.isZero()) throw new TestoraException("Invalid " + prefix + "timeout: must be greater than 0");
        if (polling.isZero()) throw new TestoraException("Invalid " + prefix + "polling: must be greater than 0");
        if (polling.compareTo(timeout) > 0) {
            throw new TestoraException("Invalid wait settings for '" + channel + "': polling (" + polling.toMillis()
                    + " ms) is longer than timeout (" + timeout.toMillis() + " ms)");
        }
        if (maxStale < 0) throw new TestoraException("Invalid " + prefix + "maxStaleRecoveries: must be 0 or more");
        return new WaitPolicy(timeout, polling, pageLoad, script, stale, noSuch, maxStale);
    }

    /** Accepts "300ms", "15s", "2m" or a plain number of seconds. */
    public static Duration parseDuration(String key, String value) {
        Matcher m = DURATION.matcher(value.trim().toLowerCase());
        if (!m.matches()) {
            throw new TestoraException("Invalid " + key + " = '" + value + "'. Use a number with ms, s or m, e.g. 300ms, 15s, 2m");
        }
        long amount = Long.parseLong(m.group(1));
        return switch (m.group(2) == null ? "s" : m.group(2)) {
            case "ms" -> Duration.ofMillis(amount);
            case "m" -> Duration.ofMinutes(amount);
            default -> Duration.ofSeconds(amount);
        };
    }

    private static Duration duration(String key, Function<String, String> lookup, Duration fallback) {
        String v = lookup.apply(key);
        return v == null || v.isBlank() ? fallback : parseDuration(key, v);
    }

    private static boolean bool(String key, Function<String, String> lookup, boolean fallback) {
        String v = lookup.apply(key);
        if (v == null || v.isBlank()) return fallback;
        String t = v.trim().toLowerCase();
        if (t.equals("true") || t.equals("false")) return Boolean.parseBoolean(t);
        throw new TestoraException("Invalid " + key + " = '" + v + "'. Use true or false");
    }

    private static int integer(String key, Function<String, String> lookup, int fallback) {
        String v = lookup.apply(key);
        if (v == null || v.isBlank()) return fallback;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new TestoraException("Invalid " + key + " = '" + v + "'. Use a whole number");
        }
    }
}
