package com.testora.ai.gateway;

import com.testora.ai.providers.AiProvider;
import com.testora.config.TestoraConfig;
import com.testora.core.utilities.Masker;
import com.testora.governance.AuditTrail;
import com.testora.observability.Metrics;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

/**
 * Single entry point to AI. Guarantees: never required, never throws, masks prompts, bounded size/time/calls,
 * provider fallback, and an audit record for every call. In OFFLINE / AI_UNAVAILABLE mode it returns empty
 * without contacting anything, so deterministic automation is unaffected.
 */
public final class AiGateway {
    public enum Mode {
        NORMAL, AI_UNAVAILABLE, OFFLINE;

        static Mode parse(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "normal" -> NORMAL;
                case "ai-unavailable" -> AI_UNAVAILABLE;
                default -> OFFLINE; // safe default
            };
        }
    }

    private static volatile AiGateway shared;

    private final Mode mode;
    private final List<AiProvider> providers;
    private final Duration timeout;
    private final int maxPromptChars;
    private final int maxCalls;
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    public AiGateway(Mode mode, List<AiProvider> providers, Duration timeout, int maxPromptChars, int maxCalls) {
        this.mode = mode;
        this.providers = providers.stream().sorted(Comparator.comparingInt(AiProvider::priority)).toList();
        this.timeout = timeout;
        this.maxPromptChars = maxPromptChars;
        this.maxCalls = maxCalls;
    }

    public static AiGateway shared() {
        if (shared == null) {
            synchronized (AiGateway.class) {
                if (shared == null) {
                    TestoraConfig c = TestoraConfig.get();
                    List<AiProvider> found = StreamSupport
                            .stream(ServiceLoader.load(AiProvider.class).spliterator(), false).toList();
                    shared = new AiGateway(Mode.parse(c.string("ai.mode", "offline")), found,
                            Duration.ofSeconds(c.integer("ai.timeout.seconds", 20)),
                            c.integer("ai.max.prompt.chars", 8000), c.integer("ai.max.calls", 50));
                }
            }
        }
        return shared;
    }

    public boolean enabled() {
        return mode == Mode.NORMAL && providers.stream().anyMatch(AiProvider::isAvailable);
    }

    public int calls() { return calls.get(); }

    public int failures() { return failures.get(); }

    public Optional<AiResponse> ask(AiRequest request) {
        if (!enabled() || calls.get() >= maxCalls) return Optional.empty();
        String prompt = Masker.mask(request.prompt());
        if (prompt.length() > maxPromptChars) prompt = prompt.substring(0, maxPromptChars);
        AiRequest safe = new AiRequest(request.purpose(), prompt, request.maxOutputTokens());
        for (AiProvider provider : providers) {
            if (!provider.isAvailable()) continue;
            calls.incrementAndGet();
            long start = System.nanoTime();
            try {
                AiResponse response = CompletableFuture.supplyAsync(() -> {
                    try {
                        return provider.complete(safe);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }).orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS).join();
                long ms = (System.nanoTime() - start) / 1_000_000;
                Metrics.recordAi(ms);
                AuditTrail.record("AI_CALL", Map.of("purpose", request.purpose(), "provider", provider.name(),
                        "model", response.model(), "tokens", response.tokens(), "latencyMs", ms, "outcome", "OK"));
                return Optional.of(response);
            } catch (RuntimeException e) {
                failures.incrementAndGet();
                AuditTrail.record("AI_CALL", Map.of("purpose", request.purpose(), "provider", provider.name(),
                        "outcome", "FAILED", "error", String.valueOf(e.getMessage())));
            }
        }
        return Optional.empty();
    }
}
