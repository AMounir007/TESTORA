# 02 - Writing Tests

## Golden rules

1. Annotate test classes with `@Testora` (`com.testora.execution.Testora`).
2. Do not create, configure or quit drivers. `DriverManager` does it lazily and per thread.
3. Do not write waits or `Thread.sleep`. `SmartElement` synchronizes every action.
4. Do not take screenshots manually. Failures capture screenshot, URL and DOM automatically.
5. Keep secrets out of code: `TestoraConfig.get().secret("ENV_VAR_NAME")`.
6. Tag every test (`smoke`, `web`, `api`, `mobile`, `e2e`, `regression`) so CI can select suites.

## Which pattern should I use?

| Pattern | Use it when | Base class |
|---|---|---|
| **Page Object** | A whole screen or page with its own URL | `BasePage` |
| **Component Object** | A reusable widget on many pages (header, table, modal) | `Component` |
| **API Client Object** | One service or resource group | `ApiClient` |
| **Screen Object (mobile)** | One mobile screen | plain class using `SmartElement.mobile(...)` |
| **Business component** (optional, yours) | A business action spanning channels, e.g. `customer.create(...)` | plain class in your own package |

Do not force every test into one pattern. Simple checks can use `SmartElement` directly.

## Web

### Page Object

```java
class LoginPage extends BasePage {
    private final SmartElement username = element(By.id("username"), "Username");
    private final SmartElement password = element(By.id("password"), "Password");
    private final SmartElement submit   = element(By.id("loginButton"), "Login Button")
            .withFallback(By.cssSelector("button[type=submit]"));   // used only if recovery is enabled

    void login(String user, String pass) {
        open("/login");          // base.url + path, waits for page ready
        username.type(user);
        password.type(pass);
        submit.click();
    }
}
```

Always give elements a readable name. It appears in error messages and reports.

### SmartElement API

| Method | What it waits for first |
|---|---|
| `click()` | present, visible, enabled |
| `type(text)` | present, visible, enabled (clears first) |
| `clear()` | present, visible, enabled |
| `getText()` | present, visible |
| `getAttribute(name)` | present |
| `verifyVisible()` | present, visible |
| `verifyText(expected)` | present, visible, text equals expected |
| `verifyInvisible()` | element absent or hidden |
| `scrollIntoView()` | present |
| `isVisible()` / `isEnabled()` | nothing. Instant check, no waiting |
| `raw()` | present. Returns the Selenium `WebElement` for advanced use |

### Browser actions (`BrowserActions`)

| Need | Call |
|---|---|
| New tab opened | `BrowserActions.switchToNewTab(2)` (expected window count) |
| Iframe | `switchToFrame("frameName")` then `leaveFrame()` |
| Alert | `BrowserActions.waitForAlert().accept()` |
| Cookie | `addCookie("name", "value")` |
| Local/session storage | `localStorage(key)`, `setLocalStorage(k, v)`, `sessionStorage(key)` |

### Advanced waits (`WaitEngine`)

```java
WaitEngine wait = new WaitEngine(DriverManager.web());
wait.waitForUrlContains("/dashboard");
wait.waitForPageReady();
wait.waitForDomStable();
wait.waitUntil("Order is shipped", d -> d.findElement(By.id("status")).getText().equals("Shipped"));
```

Use these only when `SmartElement` is not enough.

## API

Create one client class per service:

```java
class CustomerApi extends ApiClient {
    CustomerApi() { super(TestoraConfig.get().string("api.base.url", "")); }

    String create(String name) {
        var response = post("/customers", Map.of("name", name));
        assertThat(response.statusCode()).isEqualTo(201);
        return ApiClient.extractToContext(response, "id", "customer.id");  // chain to later steps
    }
}
```

| Need | Call |
|---|---|
| GET with query | `get("/customers", Map.of("page", 1))` |
| Path parameters | `get("/customers/{id}", null, id)` |
| POST / PUT / PATCH | `post(path, body)`, `put(...)`, `patch(...)` |
| DELETE | `delete("/customers/{id}", id)` |
| Bearer token | `new CustomerApi().bearer(token)` |
| API key | `.apiKey("X-API-Key", value)` |
| OAuth2 client credentials | `ApiClient.clientCredentialsToken(tokenUrl, clientId, clientSecret)` |
| File upload | `upload(path, "file", new File("..."))` |
| JSON schema check | `ApiClient.assertSchema(response, "schemas/customer.json")` (classpath file) |
| Extract value | `response.jsonPath().getString("name")` |
| Share a value across steps | `ApiClient.extractToContext(response, "id", "customer.id")` |

Every call is saved as masked evidence and counted in telemetry.

## Mobile

```java
SmartElement.mobile(AppiumBy.accessibilityId("login"), "Login button").click();
Gestures.swipeUp();
MobileApp.launch("com.example.app");
```

Set `mobile.platform` (`android` or `ios`), `mobile.device`, `mobile.app` and `appium.url` in config. See [03 - Configuration](03-configuration.md).

## Cross-channel (API, then Web, then Mobile)

All channels in one test share the same `TestContext`:

```java
@Test @Tag("e2e")
void customerCreatedByApiIsVisibleEverywhere() {
    String id = new CustomerApi().create("Ahmed");               // API
    SmartElement.web(By.cssSelector("[data-customer-id='" + id + "']"), "Customer row").verifyVisible();   // Web
    SmartElement.mobile(AppiumBy.accessibilityId("customer-" + id), "Customer cell").verifyVisible();      // Mobile
}
```

Read shared values anywhere with `TestContextHolder.current().get("customer.id")`.

## Test data

```java
DataGen data = DataGen.fromConfig();
String name  = data.text("cust", 6);
String email = data.email();
String[] boundaries = data.boundaryStrings(1, 50);   // min-1, min, max, max+1
```

Reproduce a failing run with the same data: `-Dtestdata.seed=12345`.

## Cleanup

Register anything that must be undone. It always runs, even when the test fails, newest first:

```java
String id = api.create("Temp");
CleanupRegistry.register(() -> api.delete("/customers/{id}", id));
```

## Retries (three independent kinds)

| Kind | What repeats | Controlled by |
|---|---|---|
| Synchronization | Waiting for a condition | Wait policy (timeout, polling) |
| Action | One interaction (click intercepted, stale element) | `retry.action.max` (default 2) |
| Test | The entire test | Maven Surefire `rerunFailingTestsCount` (not enabled by default) |

Retries are always bounded and reported. They never hide a deterministic failure.

## Do and don't

| Do | Don't |
|---|---|
| Name every element | Use `Thread.sleep` |
| Use `@Testora` and tags | Create drivers with `new ChromeDriver()` |
| Keep business logic in page, API or business objects | Put assertions and locators in one giant base class |
| Read secrets from environment variables | Hardcode passwords or tokens |
| Treat recovery and AI as helpers | Rely on recovery to fix a broken locator permanently |
