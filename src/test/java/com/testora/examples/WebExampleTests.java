package com.testora.examples;

import com.testora.execution.Testora;
import com.testora.synchronization.WaitEngine;
import com.testora.web.browser.BrowserActions;
import com.testora.web.driver.DriverManager;
import com.testora.web.elements.SmartElement;
import com.testora.web.pages.BasePage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runnable WEB examples against the public practice site https://the-internet.herokuapp.com.
 * Run:  mvn test -Dgroups=example
 * Needs internet access. The site is third party and can change or be unavailable; these are learning
 * examples, not part of the offline smoke suite. Note what is NOT here: no waits, no driver code,
 * no screenshots, no cleanup. The demo login below is published on the site's own login page.
 */
@Testora
@Tag("example")
@Tag("web")
class WebExampleTests {

    private static final String SITE = "https://the-internet.herokuapp.com";

    private static void go(String path) {
        DriverManager.web().get(SITE + path);
        new WaitEngine(DriverManager.web()).waitForPageReady();
    }

    /** Page Object: one class per page. */
    static class LoginPage extends BasePage {
        private final SmartElement username = element(By.id("username"), "Username");
        private final SmartElement password = element(By.id("password"), "Password");
        private final SmartElement submit = element(By.cssSelector("button[type='submit']"), "Login button");
        private final SmartElement flash = element(By.id("flash"), "Flash message");

        LoginPage open() {
            go("/login");
            return this;
        }

        void login(String user, String pass) {
            username.type(user);
            password.type(pass);
            submit.click();
        }

        String message() { return flash.getText(); }
    }

    @Test
    @Tag("smoke-web")
    void loginWithValidUser() {
        LoginPage login = new LoginPage().open();
        login.login("tomsmith", "SuperSecretPassword!");
        assertThat(login.message()).contains("You logged into a secure area!");
    }

    @Test
    void loginWithInvalidUserShowsError() {
        LoginPage login = new LoginPage().open();
        login.login("nobody", "wrong");
        assertThat(login.message()).contains("Your username is invalid!");
    }

    @Test
    void checkboxAndDropdownInteraction() {
        go("/checkboxes");
        SmartElement first = SmartElement.web(By.cssSelector("#checkboxes input:nth-of-type(1)"), "First checkbox");
        first.click();
        assertThat(first.raw().isSelected()).isTrue();

        go("/dropdown");
        SmartElement dropdown = SmartElement.web(By.id("dropdown"), "Dropdown");
        dropdown.raw().findElement(By.cssSelector("option[value='2']")).click();
        assertThat(dropdown.raw().getAttribute("value")).isEqualTo("2");
    }

    @Test
    void multipleTabs() {
        go("/windows");
        SmartElement.web(By.linkText("Click Here"), "Open new window link").click();
        BrowserActions.switchToNewTab(2);
        assertThat(SmartElement.web(By.tagName("h3"), "New window heading").getText()).isEqualTo("New Window");
    }

    @Test
    void javascriptAlert() {
        go("/javascript_alerts");
        SmartElement.web(By.cssSelector("button[onclick='jsAlert()']"), "JS alert button").click();
        BrowserActions.waitForAlert().accept();
        assertThat(SmartElement.web(By.id("result"), "Result").getText()).contains("You successfully clicked an alert");
    }

    @Test
    void nestedIframes() {
        go("/nested_frames");
        BrowserActions.switchToFrame("frame-top");
        BrowserActions.switchToFrame("frame-middle");
        assertThat(SmartElement.web(By.id("content"), "Middle frame content").getText()).isEqualTo("MIDDLE");
        BrowserActions.leaveFrame();
    }

    @Test
    void fileUpload() throws IOException {
        Path dir = Files.createTempDirectory("testora-upload");
        Path file = dir.resolve("testora-upload.txt");
        Files.writeString(file, "hello from TESTORA");
        try {
            go("/upload");
            SmartElement.web(By.id("file-upload"), "File input").raw().sendKeys(file.toString());
            SmartElement.web(By.id("file-submit"), "Upload button").click();
            assertThat(SmartElement.web(By.id("uploaded-files"), "Uploaded file name").getText())
                    .isEqualTo("testora-upload.txt");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(dir);
        }
    }

    @Test
    void dynamicContentIsWaitedForAutomatically() {
        go("/dynamic_loading/2");
        SmartElement.web(By.cssSelector("#start button"), "Start button").click();
        // The text appears after a few seconds. No wait code is needed: SmartElement waits for it.
        assertThat(SmartElement.web(By.id("finish"), "Finish text").getText()).isEqualTo("Hello World!");
    }
}
