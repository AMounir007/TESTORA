package com.testora.core.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Thread-safe JSON-lines appender. */
public final class Jsonl {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Object LOCK = new Object();

    private Jsonl() { }

    public static Path outputDir() { return Path.of("target", "testora"); }

    public static void append(Path file, Object value) {
        try {
            String line = Masker.mask(MAPPER.writeValueAsString(value)) + System.lineSeparator();
            synchronized (LOCK) {
                Files.createDirectories(file.toAbsolutePath().getParent());
                Files.writeString(file, line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            // Reporting must never break a test run.
            System.err.println("TESTORA: could not write " + file + ": " + e);
        }
    }
}
