package com.testora.web.driver;

import com.testora.config.TestoraConfig;
import com.testora.config.WaitPolicy;
import com.testora.core.exceptions.TestoraException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;

/** Creates local or remote (Grid / Docker / cloud) browsers. Selenium Manager resolves local drivers. */
final class DriverFactory {
    private DriverFactory() { }

    static WebDriver create() {
        TestoraConfig cfg = TestoraConfig.get();
        String browser = cfg.string("browser", "chrome").toLowerCase();
        boolean headless = cfg.bool("headless", true);
        String grid = cfg.string("grid.url", "");
        WebDriver driver;
        switch (browser) {
            case "chrome" -> {
                ChromeOptions o = new ChromeOptions();
                if (headless) o.addArguments("--headless=new");
                driver = grid.isBlank() ? new ChromeDriver(o) : remote(grid, o);
            }
            case "firefox" -> {
                FirefoxOptions o = new FirefoxOptions();
                if (headless) o.addArguments("-headless");
                driver = grid.isBlank() ? new FirefoxDriver(o) : remote(grid, o);
            }
            case "edge" -> {
                EdgeOptions o = new EdgeOptions();
                if (headless) o.addArguments("--headless=new");
                driver = grid.isBlank() ? new EdgeDriver(o) : remote(grid, o);
            }
            default -> throw new TestoraException("Unsupported browser '" + browser + "'. Use chrome, firefox or edge.");
        }
        WaitPolicy policy = cfg.webWait();
        driver.manage().timeouts().pageLoadTimeout(policy.pageLoadTimeout()).scriptTimeout(policy.scriptTimeout());
        return driver;
    }

    private static WebDriver remote(String url, org.openqa.selenium.Capabilities caps) {
        try {
            return new RemoteWebDriver(URI.create(url).toURL(), caps);
        } catch (MalformedURLException e) {
            throw new TestoraException("Invalid grid.url: " + url, e);
        }
    }
}
