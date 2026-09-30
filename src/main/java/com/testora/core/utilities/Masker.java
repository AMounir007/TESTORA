package com.testora.core.utilities;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Masks secrets in logs, reports, evidence and AI prompts. */
public final class Masker {
    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)(\"?(?:password|passwd|token|secret|api[-_]?key|authorization|access_token)\"?\\s*[:=]\\s*\"?)(?:Bearer\\s+)?[^\",\\s&}]+");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Set<String> SENSITIVE_HEADERS =
            Set.of("authorization", "cookie", "set-cookie", "x-api-key", "proxy-authorization");

    private Masker() { }

    public static String mask(String text) {
        if (text == null) return null;
        String out = KEY_VALUE.matcher(text).replaceAll("$1****");
        return BEARER.matcher(out).replaceAll("Bearer ****");
    }

    public static Map<String, String> maskHeaders(Map<String, String> headers) {
        Map<String, String> out = new LinkedHashMap<>();
        headers.forEach((k, v) -> out.put(k, SENSITIVE_HEADERS.contains(k.toLowerCase()) ? "****" : v));
        return out;
    }
}
