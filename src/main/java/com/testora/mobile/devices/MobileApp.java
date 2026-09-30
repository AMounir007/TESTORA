package com.testora.mobile.devices;

import com.testora.web.driver.DriverManager;
import io.appium.java_client.InteractsWithApps;

import java.util.Map;

/** Application lifecycle on the current mobile session. */
public final class MobileApp {
    private MobileApp() { }

    private static InteractsWithApps apps() { return (InteractsWithApps) DriverManager.mobile(); }

    public static void launch(String appId) { apps().activateApp(appId); }

    public static void terminate(String appId) { apps().terminateApp(appId); }

    public static void install(String path) { apps().installApp(path); }

    /** Reset = terminate then relaunch (keeps app data; use capabilities noReset/fullReset for data reset). */
    public static void restart(String appId) {
        terminate(appId);
        launch(appId);
    }

    /** Android deep link. */
    public static void deepLink(String url, String packageName) {
        ((org.openqa.selenium.JavascriptExecutor) DriverManager.mobile())
                .executeScript("mobile: deepLink", Map.of("url", url, "package", packageName));
    }
}
