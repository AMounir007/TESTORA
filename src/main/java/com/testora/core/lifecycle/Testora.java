package com.testora.core.lifecycle;

import com.testora.observability.Explainer;
import com.testora.observability.Metrics;
import com.testora.observability.MetricsListener;
import com.testora.reporting.ReportWriter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/** One-time platform start-up: installs event consumers and the shutdown report. Idempotent and thread-safe. */
public final class Testora {
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private Testora() { }

    public static void bootstrap() {
        if (!STARTED.compareAndSet(false, true)) return;
        MetricsListener.install();
        Explainer.install();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ReportWriter.writeHtml();
            LoggerFactory.getLogger("testora").info(Metrics.summary());
        }, "testora-shutdown"));
    }
}
