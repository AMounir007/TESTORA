package com.testora.web.driver;

import com.testora.execution.CleanupRegistry;
import com.testora.mobile.driver.MobileDriverFactory;
import com.testora.observability.Metrics;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

/**
 * Thread-isolated, lazily created drivers. API-only tests never start a browser.
 * Each driver registers its own cleanup, so release is guaranteed by the lifecycle.
 */
public final class DriverManager {
    private static final ThreadLocal<WebDriver> WEB = new ThreadLocal<>();
    private static final ThreadLocal<WebDriver> MOBILE = new ThreadLocal<>();

    private DriverManager() { }

    public static WebDriver web() {
        WebDriver driver = WEB.get();
        if (driver == null) {
            long start = System.nanoTime();
            driver = DriverFactory.create();
            Metrics.recordDriverCreation((System.nanoTime() - start) / 1_000_000);
            WEB.set(driver);
            CleanupRegistry.register(() -> quit(WEB));
        }
        return driver;
    }

    public static WebDriver mobile() {
        WebDriver driver = MOBILE.get();
        if (driver == null) {
            long start = System.nanoTime();
            driver = MobileDriverFactory.create();
            Metrics.recordDriverCreation((System.nanoTime() - start) / 1_000_000);
            MOBILE.set(driver);
            CleanupRegistry.register(() -> quit(MOBILE));
        }
        return driver;
    }

    public static Optional<WebDriver> webIfStarted() { return Optional.ofNullable(WEB.get()); }

    public static Optional<WebDriver> mobileIfStarted() { return Optional.ofNullable(MOBILE.get()); }

    private static void quit(ThreadLocal<WebDriver> holder) {
        WebDriver driver = holder.get();
        holder.remove();
        if (driver != null) driver.quit();
    }
}
