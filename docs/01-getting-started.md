# 01 - Getting Started

## Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| JDK | 21 or newer | Everything |
| Maven | 3.9+ | Build and test |
| Chrome, Firefox or Edge | Current | Web tests (Selenium Manager downloads the driver automatically) |
| Appium server 2.x + device/emulator | Optional | Mobile tests only |
| Selenium Grid / Docker | Optional | Remote or parallel browsers |

Check your setup:

```
java -version
mvn -version
```

## Get the code

```
git clone https://github.com/AMounir007/TESTORA.git
cd TESTORA
```

## Run the built-in platform tests

These tests need no browser, device or AI. They prove the core works on your machine.

```
mvn test -Dgroups=smoke
```

Expected: `PlatformTest` (9 tests) passes.

## Run against your application

1. Open `src/main/resources/config/qa.yaml` and set your URLs:

   ```yaml
   base.url: https://your-app.example.com
   api.base.url: https://api.your-app.example.com
   browser: chrome
   headless: "true"
   ```
2. Export any secrets as environment variables (never put them in files):

   - Windows (Command Prompt): `set DEMO_PASSWORD=yourpassword`
   - Windows (PowerShell): `$env:DEMO_PASSWORD="yourpassword"`
   - Linux/macOS: `export DEMO_PASSWORD=yourpassword`
3. Open `src/test/java/com/testora/examples/ExampleTests.java`, adapt locators to your app, and remove `@Disabled`.
4. Run:

   ```
   mvn test -Denv=qa -Dgroups=web
   ```

## Choose environment and test groups

| Command | Meaning |
|---|---|
| `-Denv=qa` | Loads `config/qa.yaml`. Use `-Denv=uat` to load `config/uat.yaml` (create the file first) |
| `-Dgroups=smoke` | Runs tests tagged `@Tag("smoke")` |
| `-Dgroups=api` / `web` / `e2e` | Runs by tag |
| `-Dbrowser=firefox` | Overrides any config key from the command line |
| `-Dheadless=false` | Shows the browser window |
| `-Dgrid.url=http://localhost:4444` | Runs browsers on a Selenium Grid |

Precedence for every setting: **command line > environment variable > YAML file**.

## Where results go

After a run, open `target/testora/report.html`. See [06 - Reports and Evidence](06-reports-and-evidence.md).

## Your first test (copy and adapt)

```java
import com.testora.execution.Testora;
import com.testora.web.elements.SmartElement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

@Testora
class MyFirstTest {

    @Test @Tag("smoke") @Tag("web")
    void homePageShowsTitle() {
        SmartElement.web(By.tagName("h1"), "Page title").verifyVisible();
    }
}
```

No driver setup, waits, screenshots or cleanup are needed.
