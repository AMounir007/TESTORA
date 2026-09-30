package com.testora.execution.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testora.config.TestoraConfig;
import com.testora.core.utilities.Jsonl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Append-only execution history in JSON lines. Default file: .testora/history.jsonl (key: history.file).
 * Survives "mvn clean" because it lives outside target/. In CI, keep it between runs as a cache or artifact,
 * otherwise each run starts with empty history.
 */
public final class HistoryStore {
    /** Identifies this JVM run, so "earlier runs" can be told apart from the current one. */
    public static final String RUN_ID = UUID.randomUUID().toString();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HistoryStore() { }

    public static Path file() {
        return Path.of(TestoraConfig.get().string("history.file", ".testora/history.jsonl"));
    }

    public static void append(String testId, String status, long durationMs, int retries, int recoveries,
                              String category, String fingerprint) {
        if (!TestoraConfig.get().bool("history.enabled", true)) return;
        Jsonl.append(file(), new HistoryRecord(RUN_ID, testId, status, Instant.now().toString(), durationMs,
                retries, recoveries, category, fingerprint));
    }

    /** All records, oldest first. Unreadable lines are skipped; a missing file means no history. */
    public static List<HistoryRecord> readAll() {
        Path file = file();
        List<HistoryRecord> all = new ArrayList<>();
        if (!Files.exists(file)) return all;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                try {
                    all.add(MAPPER.readValue(line, HistoryRecord.class));
                } catch (IOException badLine) {
                    // skip corrupt line, keep the rest
                }
            }
        } catch (IOException e) {
            System.err.println("TESTORA: could not read history " + file + ": " + e);
        }
        return all;
    }
}
