package com.testora.web.browser;

import com.testora.synchronization.WaitEngine;
import com.testora.web.driver.DriverManager;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

/** Browser-level operations: tabs, frames, alerts, cookies, storage. */
public final class BrowserActions {
    private BrowserActions() { }

    private static WebDriver d() { return DriverManager.web(); }

    public static String currentWindow() { return d().getWindowHandle(); }

    /** Waits for the expected number of windows, then switches to the newest one. */
    public static void switchToNewTab(int expectedCount) {
        new WaitEngine(d()).waitUntil("WINDOW_COUNT " + expectedCount, x -> x.getWindowHandles().size() == expectedCount);
        List<String> handles = new ArrayList<>(d().getWindowHandles());
        d().switchTo().window(handles.get(handles.size() - 1));
    }

    public static void switchToWindow(String handle) { d().switchTo().window(handle); }

    public static void switchToFrame(String nameOrId) {
        new WaitEngine(d()).waitUntil("FRAME " + nameOrId, x -> {
            x.switchTo().frame(nameOrId);
            return true;
        });
    }

    public static void leaveFrame() { d().switchTo().defaultContent(); }

    public static Alert waitForAlert() {
        return new WaitEngine(d()).waitUntil("ALERT", x -> x.switchTo().alert());
    }

    public static void addCookie(String name, String value) { d().manage().addCookie(new Cookie(name, value)); }

    public static String localStorage(String key) {
        return (String) ((JavascriptExecutor) d()).executeScript("return window.localStorage.getItem(arguments[0])", key);
    }

    public static void setLocalStorage(String key, String value) {
        ((JavascriptExecutor) d()).executeScript("window.localStorage.setItem(arguments[0], arguments[1])", key, value);
    }

    public static String sessionStorage(String key) {
        return (String) ((JavascriptExecutor) d()).executeScript("return window.sessionStorage.getItem(arguments[0])", key);
    }
}
