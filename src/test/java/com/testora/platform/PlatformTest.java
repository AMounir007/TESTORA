package com.testora.platform;

import com.testora.ai.gateway.AiGateway;
import com.testora.ai.gateway.AiRequest;
import com.testora.ai.gateway.AiResponse;
import com.testora.ai.providers.AiProvider;
import com.testora.core.events.EventBus;
import com.testora.core.events.EventType;
import com.testora.core.events.TestoraEvent;
import com.testora.core.utilities.Masker;
import com.testora.intelligence.fingerprinting.FailureClassifier;
import com.testora.intelligence.impact.FlakinessAnalyzer;
import com.testora.intelligence.knowledge.KnowledgeGraph;
import com.testora.intelligence.selection.TestSelector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline platform tests: they prove Level 1/2 behaviour needs no browser, device or AI. */
@Tag("smoke")
class PlatformTest {

    private static AiProvider provider(String name, boolean fail, AtomicInteger seenSecrets) {
        return new AiProvider() {
            @Override public String name() { return name; }
            @Override public boolean isAvailable() { return true; }
            @Override public AiResponse complete(AiRequest r) throws Exception {
                if (r.prompt().contains("hunter2")) seenSecrets.incrementAndGet();
                if (fail) throw new IllegalStateException("provider down");
                return new AiResponse("UNKNOWN - Insufficient Evidence", name, "m", 10, 1);
            }
        };
    }

    @Test
    void masksSecrets() {
        assertThat(Masker.mask("password=hunter2&x=1")).doesNotContain("hunter2");
        assertThat(Masker.mask("Authorization: Bearer abc.def.ghi")).doesNotContain("abc.def.ghi");
    }

    @Test
    void eventBusIsolatesFailingListeners() {
        AtomicInteger received = new AtomicInteger();
        EventBus.global().subscribe(EventType.TEST_STARTED, e -> { throw new IllegalStateException("boom"); });
        EventBus.global().subscribe(EventType.TEST_STARTED, e -> received.incrementAndGet());
        EventBus.global().publish(TestoraEvent.of(EventType.TEST_STARTED, "x", "y", Map.of()));
        assertThat(received.get()).isEqualTo(1);
    }

    @Test
    void classifiesAssertionAsLowConfidenceApplicationDefect() {
        var c = FailureClassifier.classify(new AssertionError("expected 1 but was 2"));
        assertThat(c.category()).isEqualTo(FailureClassifier.Category.APPLICATION_DEFECT);
        assertThat(c.confidence()).isLessThan(0.8);
    }

    @Test
    void sameFailureProducesSameFingerprint() {
        var a = FailureClassifier.classify(new AssertionError("expected 1 but was 22"));
        var b = FailureClassifier.classify(new AssertionError("expected 5 but was 7"));
        assertThat(a.fingerprint()).isEqualTo(b.fingerprint());
    }

    @Test
    void unknownFailureIsHonest() {
        var c = FailureClassifier.classify(new IllegalStateException("weird"));
        assertThat(c.category()).isEqualTo(FailureClassifier.Category.UNKNOWN);
        assertThat(c.evidence()).contains("Insufficient Evidence");
    }

    @Test
    void flakinessNeedsMixedOutcomes() {
        var history = new java.util.ArrayList<Boolean>();
        for (int i = 0; i < 16; i++) history.add(true);
        for (int i = 0; i < 4; i++) history.add(false);
        assertThat(FlakinessAnalyzer.analyze(history, 20, 0.1).flaky()).isTrue();
        assertThat(FlakinessAnalyzer.analyze(List.of(false, false, false), 20, 0.1).flaky()).isFalse();
    }

    @Test
    void offlineGatewayNeverCallsProviders() {
        AtomicInteger secrets = new AtomicInteger();
        var gw = new AiGateway(AiGateway.Mode.OFFLINE, List.of(provider("a", false, secrets)), Duration.ofSeconds(1), 1000, 5);
        assertThat(gw.ask(new AiRequest("t", "hi", 10))).isEmpty();
        assertThat(gw.calls()).isZero();
    }

    @Test
    void gatewayFallsBackAndMasksPrompts() {
        AtomicInteger secrets = new AtomicInteger();
        var gw = new AiGateway(AiGateway.Mode.NORMAL,
                List.of(provider("primary", true, secrets), provider("backup", false, secrets)),
                Duration.ofSeconds(2), 1000, 5);
        var answer = gw.ask(new AiRequest("t", "password=hunter2", 10));
        assertThat(answer).isPresent();
        assertThat(answer.get().provider()).isEqualTo("backup");
        assertThat(secrets.get()).isZero();
    }

    @Test
    void impactAndSelectionAreExplainable() {
        var graph = new KnowledgeGraph().link("api:customer", "web:customerPage").link("web:customerPage", "test:CreateCustomer");
        Set<String> impacted = graph.affectedBy("api:customer", "test:");
        assertThat(impacted).containsExactly("test:CreateCustomer");
        var plan = TestSelector.select(List.of(new TestSelector.Candidate("test:CreateCustomer", 0.4, 0),
                new TestSelector.Candidate("test:Other", 0.9, 0)), impacted, 1);
        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).reason()).contains("Impacted");
    }
}
