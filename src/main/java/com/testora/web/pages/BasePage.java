package com.testora.web.pages;

import com.testora.config.TestoraConfig;
import com.testora.synchronization.WaitEngine;
import com.testora.web.driver.DriverManager;
import com.testora.web.elements.SmartElement;
import org.openqa.selenium.By;

/**
 * Thin page base: navigation and element factory only (no giant BaseTest/utility).
 * Use Page Objects for whole pages, Component Objects (see {@link com.testora.web.components.Component})
 * for reusable widgets like headers or tables.
 */
public abstract class BasePage {
    protected SmartElement element(By by, String name) { return SmartElement.web(by, name); }

    public void open(String path) {
        DriverManager.web().get(TestoraConfig.get().string("base.url", "") + path);
        new WaitEngine(DriverManager.web()).waitForPageReady();
    }

    public void waitForUrlContains(String fragment) {
        new WaitEngine(DriverManager.web()).waitForUrlContains(fragment);
    }
}
