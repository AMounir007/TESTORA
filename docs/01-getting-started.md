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

Expected: 18 tests pass (`PlatformTest` 9, `HistoryTrendTest` 1, `ContractAnalyzerTest` 3, `WaitPolicyTest` 5). `ExampleTests` is disabled and does not run.

You can also run this from IntelliJ: open the **Maven** tool window, click **Reload All Maven Projects**, then double-click **clean** and **test** under TESTORA → Lifecycle.

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

## How to start automating your application

Follow these steps in order. Each one links to the detailed guide.

1. **Pick what to automate first.** Start with 3-5 stable, high-value flows (login, create a record, search). Prefer API checks where possible because they are fastest and least flaky; add Web or Mobile checks for what users actually see.
2. **Point the framework at your app.** Set `base.url` and `api.base.url` in `config/qa.yaml` and export secrets as environment variables (see [Run against your application](#run-against-your-application) and [03 - Configuration](03-configuration.md)).
3. **Choose a pattern.** Page Object for a page, Component Object for a reusable widget, API Client Object for a service (see [02 - Writing Tests](02-writing-tests.md#which-pattern-should-i-use)).
4. **Write one small test.** Annotate the class with `@Testora`, tag it (`smoke`, `web`, `api`...), and use `SmartElement` or an API client. Do not add drivers, waits or screenshots. Follow the [Golden rules](02-writing-tests.md#golden-rules).
5. **Run it locally.** `mvn test -Denv=qa -Dgroups=smoke`, then open `target/testora/report.html` ([06 - Reports and Evidence](06-reports-and-evidence.md)).
6. **Make it reliable.** Run it several times; fix any failure before adding more tests. Keep tests independent, with their own test data.
7. **Grow gradually.** Add one flow at a time, then cross-channel (API, then Web, then Mobile) tests once the basics are stable.
8. **Run it in CI.** Run the `smoke` suite on every push and the full regression nightly (see [11 - Continuous Integration](11-continuous-integration.md)).

If something fails, see [07 - Troubleshooting](07-troubleshooting.md).

## Try the ready-made examples

Runnable Web and API examples against public practice sites are in `src/test/java/com/testora/examples/`:

```
mvn test -Dgroups=web        # WebExampleTests (the-internet.herokuapp.com)
mvn test -Dgroups=api        # ApiExampleTests (jsonplaceholder.typicode.com)
mvn test -Dgroups=example    # both
```

They need internet access. See [12 - Example Tests](12-example-tests.md).
