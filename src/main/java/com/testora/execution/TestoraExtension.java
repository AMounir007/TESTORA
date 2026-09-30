package com.testora.execution;

import com.testora.ai.diagnosis.FailureAnalyzer;
import com.testora.ai.gateway.AiGateway;
import com.testora.core.context.TestContext;
import com.testora.core.context.TestContextHolder;
import com.testora.core.events.EventType;
import com.testora.core.lifecycle.Testora;
import com.testora.config.TestoraConfig;
import com.testora.evidence.EvidenceService;
import com.testora.execution.history.HistoryStore;
import com.testora.intelligence.fingerprinting.FailureClassifier;
import com.testora.intelligence.fingerprinting.FailureClassifier.Classification;
import com.testora.observability.Explainer;
import com.testora.reporting.ReportWriter;
import com.testora.reporting.TestSummary;
import com.testora.web.driver.DriverManager;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Drives SETUP -> EXECUTION -> EVIDENCE -> CLEANUP for every test.
 * JUnit runs beforeEach, the test body and afterEach on one thread, so the ThreadLocal context is isolated per
 * parallel test. Cleanup always runs. Evidence is captured BEFORE drivers are released.
 */
public final class TestoraExtension implements BeforeEachCallback, AfterEachCallback, TestExecutionExceptionHandler {

    @Override
    public void beforeEach(ExtensionContext junit) {
        Testora.bootstrap();
        String testId = junit.getRequiredTestClass().getSimpleName() + "." + junit.getRequiredTestMethod().getName();
        TestContext ctx = new TestContext(testId, TestoraConfig.get().env());
        TestContextHolder.set(ctx);
        ctx.publish(EventType.TEST_STARTED, Map.of("test", testId));
        ctx.publish(EventType.SETUP_STARTED, Map.of("test", testId));
    }

    @Override
    public void handleTestExecutionException(ExtensionContext junit, Throwable failure) throws Throwable {
        TestContext ctx = TestContextHolder.current();
        ctx.put("failure", failure);
        DriverManager.webIfStarted().ifPresent(d -> EvidenceService.captureWeb(d, "failure"));
        DriverManager.mobileIfStarted().ifPresent(d -> EvidenceService.captureMobile(d, "failure-mobile"));
        FailureAnalyzer.Analysis analysis = FailureAnalyzer.analyze(failure, AiGateway.shared());
        ctx.put("classification", analysis.classification());
        analysis.aiInference().ifPresent(ctx::aiAnalysis);
        ctx.publish(EventType.TEST_FAILED, Map.of("category", analysis.classification().category().name(),
                "fingerprint", analysis.classification().fingerprint()));
        throw failure;
    }

    @Override
    public void afterEach(ExtensionContext junit) {
        TestContext ctx = TestContextHolder.current();
        try {
            ctx.publish(EventType.CLEANUP_STARTED, Map.of());
            CleanupRegistry.runAll();
            ctx.complete();
            boolean failed = ctx.get("failure").isPresent();
            long ms = Duration.between(ctx.startTime(), ctx.endTime().orElse(Instant.now())).toMillis();
            if (!failed) ctx.publish(EventType.TEST_PASSED, Map.of());
            ctx.publish(EventType.TEST_COMPLETED, Map.of("durationMs", String.valueOf(ms)));
            Classification c = ctx.<Classification>get("classification").orElse(null);
            ReportWriter.add(new TestSummary(ctx.testId(), failed ? "FAILED" : "PASSED", ms, ctx.retries(),
                    ctx.recoveries(), c == null ? "-" : c.category().name(), c == null ? 0 : c.confidence(),
                    c == null ? "-" : c.fingerprint(), ctx.evidence(), Explainer.why(ctx.executionId()),
                    ctx.aiAnalysis().orElse("")));
            Explainer.forget(ctx.executionId());
            HistoryStore.append(ctx.testId(), failed ? "FAILED" : "PASSED", ms, ctx.retries(), ctx.recoveries(),
                    c == null ? "-" : c.category().name(), c == null ? "-" : c.fingerprint());
        } finally {
            TestContextHolder.clear();
        }
    }
}
