# 12 - Example Tests (copy and adapt)

Ready-to-run examples live in `src/test/java/com/testora/examples/`. Testers can copy a class, change the locators and URLs, and have a working test.

| File | What it shows | Runs against | Command |
|---|---|---|---|
| `WebExampleTests` | Page Object, login (valid and invalid), checkbox, dropdown, multiple tabs, JavaScript alert, nested iframes, file upload, dynamic content | https://the-internet.herokuapp.com (public practice site) | `mvn test -Dgroups=web` |
| `ApiExampleTests` | API client object, GET, POST, PUT, PATCH, DELETE, query parameters, JSON schema validation, request chaining through `TestContext`, negative cases | https://jsonplaceholder.typicode.com (public fake API) | `mvn test -Dgroups=api` |
| `ExampleTests` | Template for **your own** application: page object, API client, API then Web then Mobile workflow | Your app. Disabled until you configure it | see below |
| `PlatformTest`, `HistoryTrendTest`, `ContractAnalyzerTest`, `WaitPolicyTest` | Tests of TESTORA itself (offline) | Nothing external | `mvn test -Dgroups=smoke` |

Run all public examples: `mvn test -Dgroups=example`.

Notes:
- They need internet access. The sites are third party and can change or go offline, so treat a failure there as a possible site change first. They are not part of the offline smoke suite or the CI build.
- The demo login (`tomsmith`) is published on that site's login page. For your own systems, read credentials with `TestoraConfig.get().secret("NAME")`.
- JSONPlaceholder does not store writes, so a created post cannot be read back.
- Mobile, permissions and gestures examples are not included: they need an Appium server and a device, which cannot be assumed. The building blocks are `SmartElement.mobile(...)`, `Gestures` and `MobileApp` (see [02](02-writing-tests.md)).
- Authentication example: the public API has no login. Use `new ApiClient(url).bearer(token)` or `ApiClient.clientCredentialsToken(...)` for real services.
- The examples were written without being run in the authoring environment, so the first run may need small locator fixes.

## Pattern to copy

```java
@Testora                       // context, evidence, cleanup, report, history
@Tag("web")                    // used by -Dgroups=web
class MyCustomerTests {

    static class CustomerPage extends BasePage {           // Page Object
        private final SmartElement name = element(By.id("name"), "Name");
        private final SmartElement save = element(By.id("save"), "Save button");
3. Remove `@Disabled` from the class and run `mvn test -Dgroups=smoke` or your own tag.
2. Change the locators and API paths to match your application.
1. Set `base.url` and `api.base.url` in `qa.yaml`, and export the `DEMO_PASSWORD`-style secrets it reads.

## Adapting `ExampleTests` for your application

No waits, driver setup, screenshots or cleanup appear in the test. To use your own system, set `base.url` and `api.base.url` in `src/main/resources/config/qa.yaml`.

```
}
    }
        SmartElement.web(By.id("success"), "Success message").verifyVisible();
        new CustomerPage().create(DataGen.fromConfig().text("cust", 6));
    void createCustomer() {
    @Test

    }
        void create(String value) { open("/customers/new"); name.type(value); save.click(); }
