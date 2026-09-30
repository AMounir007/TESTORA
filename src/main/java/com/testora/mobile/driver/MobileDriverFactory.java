package com.testora.mobile.driver;

import com.testora.config.TestoraConfig;
import com.testora.core.exceptions.TestoraException;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/** Creates Android/iOS sessions against a local Appium server or any Appium-compatible cloud. */
public final class MobileDriverFactory {
    private MobileDriverFactory() { }

    public static WebDriver create() {
        TestoraConfig cfg = TestoraConfig.get();
        String platform = cfg.string("mobile.platform", "android").toLowerCase();
        String device = cfg.string("mobile.device", "");
        String app = cfg.string("mobile.app", "");
        URL server = url(cfg.string("appium.url", "http://127.0.0.1:4723"));
        switch (platform) {
            case "android" -> {
                UiAutomator2Options o = new UiAutomator2Options().setAutoGrantPermissions(cfg.bool("mobile.autoGrantPermissions", true));
                if (!device.isBlank()) o.setDeviceName(device);
                if (!app.isBlank()) o.setApp(app);
                return new AndroidDriver(server, o);
            }
            case "ios" -> {
                XCUITestOptions o = new XCUITestOptions();
                if (!device.isBlank()) o.setDeviceName(device);
                if (!app.isBlank()) o.setApp(app);
                return new IOSDriver(server, o);
            }
            default -> throw new TestoraException("Unsupported mobile.platform '" + platform + "'. Use android or ios.");
        }
    }

    private static URL url(String value) {
        try {
            return URI.create(value).toURL();
        } catch (MalformedURLException e) {
            throw new TestoraException("Invalid appium.url: " + value, e);
        }
    }
}
