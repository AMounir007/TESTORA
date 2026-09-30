package com.testora.web.elements;

import com.testora.ai.recovery.LocatorRecovery;
import com.testora.config.TestoraConfig;
import com.testora.config.WaitPolicy;
import com.testora.core.context.TestContext;
import com.testora.core.context.TestContextHolder;
import com.testora.core.events.EventType;
import com.testora.core.events.Events;
import com.testora.core.exceptions.SynchronizationException;
import com.testora.core.exceptions.TestoraException;
import com.testora.synchronization.Conditions;
import com.testora.synchronization.ElementCondition;
import com.testora.synchronization.RetryPolicy;
import com.testora.synchronization.WaitEngine;
import com.testora.web.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Smart element for Web and Mobile. Every interaction goes through WaitEngine; testers never write waits.
 * Values typed are never logged (they may be secrets).
 */
public final class SmartElement {
    private final Supplier<WebDriver> driver;
    private final By by;
    private final String name;
    private final List<By> fallbacks = new ArrayList<>();

    private SmartElement(Supplier<WebDriver> driver, By by, String name) {
        this.driver = driver;
        this.by = by;
        this.name = name;
    }

    public static SmartElement web(By by, String name) { return new SmartElement(DriverManager::web, by, name); }

    public static SmartElement mobile(By by, String name) { return new SmartElement(DriverManager::mobile, by, name); }

    /** Declares alternative locators used only by governed Controlled Recovery (if enabled). */
    public SmartElement withFallback(By... locators) {
        fallbacks.addAll(List.of(locators));
        return this;
    }

    public void click() { act("CLICK", e -> { e.click(); return null; }, Conditions.VISIBLE, Conditions.ENABLED); }

    public void type(String text) {
        act("TYPE", e -> { e.clear(); e.sendKeys(text); return null; }, Conditions.VISIBLE, Conditions.ENABLED);
    }

    public void clear() { act("CLEAR", e -> { e.clear(); return null; }, Conditions.VISIBLE, Conditions.ENABLED); }

    public String getText() { return act("GET_TEXT", WebElement::getText, Conditions.VISIBLE); }

    public String getAttribute(String attribute) { return act("GET_ATTRIBUTE", e -> e.getAttribute(attribute)); }

    public void scrollIntoView() {
        act("SCROLL", e -> ((JavascriptExecutor) driver.get())
                .executeScript("arguments[0].scrollIntoView({block:'center'})", e));
    }

    /** Non-blocking state check (no waiting). */
    public boolean isVisible() {
        try {
            List<WebElement> found = driver.get().findElements(by);
            return !found.isEmpty() && found.get(0).isDisplayed();
        } catch (StaleElementReferenceException e) {
            return false;
        }
    }

    public boolean isEnabled() {
        List<WebElement> found = driver.get().findElements(by);
        return !found.isEmpty() && found.get(0).isEnabled();
    }

    public void verifyVisible() { act("VERIFY_VISIBLE", e -> e, Conditions.VISIBLE); }

    public void verifyText(String expected) { act("VERIFY_TEXT", e -> e, Conditions.VISIBLE, Conditions.text(expected)); }

    public void verifyInvisible() { new WaitEngine(driver.get()).waitForInvisible(by); }

    /** Advanced access to the raw element (synchronized for presence only). */
    public WebElement raw() { return act("RAW", e -> e); }

    private <T> T act(String operation, Function<WebElement, T> action, ElementCondition... conditions) {
        RetryPolicy retry = RetryPolicy.action();
        TestContext ctx = TestContextHolder.optional();
        Events.emit(EventType.ACTION_STARTED, "operation", operation, "element", name);
        if (ctx != null) ctx.recordAction(operation + " " + name);
        int attempt = 0;
        while (true) {
            attempt++;
            try {
}
    }
        }
                    .orElseThrow(() -> original);
            return LocatorRecovery.recover(engine, operation, name, by, fallbacks, conditions)
        } catch (SynchronizationException original) {
            return engine.waitForElement(operation, name, by, conditions);
        try {
        WaitEngine engine = new WaitEngine(driver.get());
    private WebElement resolve(String operation, ElementCondition... conditions) {

    }
        }
            }
                throw e;
                Events.emit(EventType.ACTION_FAILED, "operation", operation, "element", name, "reason", "SynchronizationTimeout");
            } catch (SynchronizationException e) {
                        "reason", e.getClass().getSimpleName(), "attempt", attempt);
                Events.emit(EventType.RETRY_STARTED, "operation", operation, "element", name,
                if (ctx != null) ctx.incrementRetries();
                }
                            + "\n  Last State: " + e.getClass().getSimpleName() + " (blocked or re-rendered)", e);
                            + "\n  Element: " + name + "\n  Locator: " + by + "\n  Attempts: " + attempt
                    throw new TestoraException("Element Interaction Failed\n  Operation: " + operation
                    Events.emit(EventType.ACTION_FAILED, "operation", operation, "element", name, "reason", e.getClass().getSimpleName());
                if (attempt >= retry.maxAttempts()) {
            } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
                return result;
                Events.emit(EventType.ACTION_COMPLETED, "operation", operation, "element", name, "attempt", attempt);
                T result = action.apply(element);
                WebElement element = resolve(operation, conditions);
