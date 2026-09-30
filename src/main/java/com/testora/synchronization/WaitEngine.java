package com.testora.synchronization;

import com.testora.config.TestoraConfig;
import com.testora.config.WaitPolicy;
import com.testora.core.events.EventType;
import com.testora.core.events.Events;
import com.testora.core.exceptions.SynchronizationException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.List;
import java.util.function.Function;

/**
 * Central synchronization engine built on FluentWait. Used by Web and Mobile.
 * Stale elements are re-located on every poll; total time is always bounded by the policy timeout.
 * Never uses Thread.sleep.
 */
public final class WaitEngine {
    private final WebDriver driver;
    private final WaitPolicy policy;

    public WaitEngine(WebDriver driver) { this(driver, TestoraConfig.get().webWait()); }

    public WaitEngine(WebDriver driver, WaitPolicy policy) {
        this.driver = driver;
        this.policy = policy;
    }

    public WebDriver driver() { return driver; }

    private FluentWait<WebDriver> fluent() {
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(policy.timeout())
                .pollingEvery(policy.pollingInterval());
        if (policy.ignoreNoSuchElement()) wait.ignoring(NoSuchElementException.class);
        if (policy.ignoreStale()) wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    /** Locate -> present -> each condition in order. Returns as soon as all are satisfied. */
    public WebElement waitForElement(String operation, String name, By by, ElementCondition... conditions) {
        WaitDiagnostics diag = new WaitDiagnostics(operation, name, by.toString(), policy);
        Events.emit(EventType.WAIT_STARTED, "operation", operation, "element", name);
        long start = System.nanoTime();
        try {
            WebElement element = fluent().until(d -> {
                diag.nextAttempt();
                List<WebElement> found = d.findElements(by);
                diag.condition("Present", !found.isEmpty());
                if (found.isEmpty()) {
                    diag.state("Element not present");
                    return null;
                }
                WebElement candidate = found.get(0);
                for (ElementCondition c : conditions) {
                    boolean ok = c.test(candidate);
                    diag.condition(c.name(), ok);
                    if (!ok) {
                        diag.state("Element present but not " + c.name());
                        return null;
                    }
                }
                diag.state("Conditions satisfied");
                return candidate;
            });
            completed(diag, operation, name, start, "SUCCESS");
            return element;
        } catch (TimeoutException e) {
            completed(diag, operation, name, start, "TIMEOUT");
            throw new SynchronizationException(diag.report(), e);
        }
    }

    /** Advanced: wait for any custom condition. */
    public <T> T waitUntil(String operation, Function<WebDriver, T> condition) {
        WaitDiagnostics diag = new WaitDiagnostics(operation, "n/a", "n/a", policy);
        Events.emit(EventType.WAIT_STARTED, "operation", operation, "element", "n/a");
        long start = System.nanoTime();
        try {
            T result = fluent().until(d -> {
                diag.nextAttempt();
                T value = condition.apply(d);
                diag.state(value == null || Boolean.FALSE.equals(value) ? "Condition not met" : "Conditions satisfied");
                return value;
            });
            completed(diag, operation, "n/a", start, "SUCCESS");
            return result;
        } catch (TimeoutException e) {
            completed(diag, operation, "n/a", start, "TIMEOUT");
            throw new SynchronizationException(diag.report(), e);
        }
    }

    public void waitForUrlContains(String fragment) {
        waitUntil("URL_CONTAINS " + fragment, d -> d.getCurrentUrl() != null && d.getCurrentUrl().contains(fragment));
    }

    public void waitForPageReady() {
        waitUntil("PAGE_READY", d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    public void waitForInvisible(By by) {
        waitUntil("INVISIBLE " + by, d -> {
            List<WebElement> found = d.findElements(by);
            try {
                return found.isEmpty() || !found.get(0).isDisplayed();
            } catch (StaleElementReferenceException e) {
                return true;
            }
        });
    }

    /** DOM is stable when its size is unchanged for three consecutive polls. */
    public void waitForDomStable() {
        long[] last = {-1};
        int[] stable = {0};
        waitUntil("DOM_STABLE", d -> {
            Object size = ((JavascriptExecutor) d).executeScript("return document.documentElement.outerHTML.length");
            long current = ((Number) size).longValue();
            stable[0] = current == last[0] ? stable[0] + 1 : 0;
            last[0] = current;
            return stable[0] >= 2;
        });
    }

    private void completed(WaitDiagnostics diag, String operation, String name, long startNanos, String outcome) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000;
        Events.emit(EventType.WAIT_COMPLETED, "operation", operation, "element", name,
                "attempts", diag.attempts(), "durationMs", ms, "outcome", outcome,
                "pollingMs", policy.pollingInterval().toMillis(), "waitReason", diag.firstBlocker());
    }
}
