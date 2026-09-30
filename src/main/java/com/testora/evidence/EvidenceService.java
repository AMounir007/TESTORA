package com.testora.evidence;

import com.testora.core.context.TestContext;
import com.testora.core.context.TestContextHolder;
import com.testora.core.events.EventType;
import com.testora.core.events.Events;
import com.testora.core.utilities.Masker;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Captures and stores evidence per execution. Text evidence is always masked. Never throws. */
public final class EvidenceService {
    private EvidenceService() { }

    private static Path dir() throws IOException {
        TestContext ctx = TestContextHolder.optional();
        Path dir = Path.of("target", "testora", "evidence", ctx == null ? "no-context" : ctx.executionId());
        return Files.createDirectories(dir);
    }

    private static String safe(String label) { return label.replaceAll("[^A-Za-z0-9._-]", "_"); }

    public static Optional<Path> saveText(String label, String content) {
        try {
            Path file = dir().resolve(safe(label) + ".txt");
            Files.writeString(file, Masker.mask(content), StandardCharsets.UTF_8);
            registered(label, file);
            return Optional.of(file);
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public static Optional<Path> screenshot(WebDriver driver, String label) {
        try {
            byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path file = dir().resolve(safe(label) + ".png");
            Files.write(file, bytes);
            registered(label, file);
            return Optional.of(file);
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Web evidence: screenshot + URL + DOM. */
    public static void captureWeb(WebDriver driver, String label) {
        screenshot(driver, label + "-screenshot");
        try {
            saveText(label + "-url", driver.getCurrentUrl());
            saveText(label + "-dom", driver.getPageSource());
        } catch (RuntimeException ignored) {
            // browser may already be dead; evidence capture is best effort
        }
    }

    /** Mobile evidence: screenshot + page source. */
    public static void captureMobile(WebDriver driver, String label) {
        screenshot(driver, label + "-screenshot");
        try {
            saveText(label + "-source", driver.getPageSource());
        } catch (RuntimeException ignored) {
            // best effort
        }
    }

    private static void registered(String label, Path file) {
        TestContext ctx = TestContextHolder.optional();
        if (ctx != null) ctx.addEvidence(file.toString());
        Events.emit(EventType.EVIDENCE_CAPTURED, "label", label, "file", file);
    }
}
