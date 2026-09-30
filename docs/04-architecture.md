# 04 - Architecture

## Layers

```
Your tests
   |
Business components (optional, yours)       e.g. customer.create(...)
   |
Page / Component / API client / Screen objects
   |
SmartElement   ApiClient   Gestures   BrowserActions
   |
WaitEngine (FluentWait)  <-- all synchronization lives here
   |
Selenium   RestAssured   Appium

Every layer publishes events --> EventBus --> logging, metrics, explainer, reporting, evidence, AI analysis
```

The core (`core`, `config`, `synchronization`) contains no business or domain terms. Business logic lives above it.

## Three intelligence levels

| Level | Examples | Needs AI? |
|---|---|---|
| 1 - Deterministic | Selenium, RestAssured, Appium, waits, config, data | No |
| 2 - Intelligent (rules and history) | Failure classifier, fingerprinting, flakiness, risk, selection, knowledge graph | No |
| 3 - AI | Failure analysis (implemented), generation and more (planned) | Optional |

AI modes: **normal** (all levels), **ai-unavailable** and **offline** (levels 1 and 2). Tests behave the same in every mode.

## Packages

| Package (`com.testora....`) | Responsibility |
|---|---|
| `core.context` | `TestContext` (per-test state), `TestContextHolder` (thread binding) |
| `core.events` | `EventBus`, `EventType`, `TestoraEvent`, `Events` helper |
| `core.exceptions` | `TestoraException`, `SynchronizationException` |
| `core.lifecycle` | `Testora.bootstrap()` one-time startup |
| `core.utilities` | `Masker` (secret masking), `Jsonl` (thread-safe writer) |
| `config` | `TestoraConfig`, `WaitPolicy` |
| `synchronization` | `WaitEngine`, `Conditions`, `WaitDiagnostics`, `RetryPolicy` |
| `web` | `driver` (factory, manager), `elements` (SmartElement), `pages`, `components`, `browser` |
| `api.clients` | `ApiClient` |
| `mobile` | `driver`, `gestures`, `devices` (app lifecycle) |
| `execution` | `@Testora`, `TestoraExtension`, `CleanupRegistry`, `Preflight` |
| `evidence` | `EvidenceService` |
| `reporting` | `ReportWriter`, `TestSummary` |
| `observability` | `Metrics`, `MetricsListener`, `Explainer` |
| `intelligence` | `fingerprinting`, `impact` (flakiness), `risk`, `selection`, `knowledge` |
| `ai` | `gateway`, `providers` (SPI), `diagnosis`, `recovery` |
| `governance` | `ConfidencePolicy`, `ApprovalQueue`, `AuditTrail` |
| `testdata` | `DataGen` |

## Test lifecycle

```
beforeEach : create TestContext, TEST_STARTED, SETUP_STARTED
test body  : actions publish ACTION_*, WAIT_*, RETRY_*, RECOVERY_* events
on failure : capture evidence, classify, optional AI analysis, TEST_FAILED
afterEach  : CLEANUP_STARTED, run cleanups (always), quit drivers,
             TEST_PASSED, TEST_COMPLETED, write report row
at JVM exit: write report.html, log telemetry summary
```

## TestContext

Holds execution ID, correlation ID, test ID, environment, start/end time, shared data, actions, evidence paths, retry and recovery counts and the AI analysis. It is bound to the current thread, so parallel tests cannot see each other's data.

## Synchronization

`SmartElement.click()` does one polling loop:

```
locate -> present? -> visible? -> enabled? -> interact
```

- Uses Selenium `FluentWait` with timeout and polling from `WaitPolicy`.
- Returns as soon as the conditions are true (never waits the full timeout unnecessarily).
- Re-locates the element on every poll, so stale elements are handled.
- On timeout throws `SynchronizationException` with a diagnostics report:

```
Synchronization Timeout
  Operation: CLICK
  Element: Login Button
  Locator: By.id: loginButton
  Timeout: 15 seconds
  Polling: 300 ms
  Conditions:
    Present = YES
    Visible = YES
    Enabled = NO
  Attempts: 42
  Last State: Element present but not Enabled
  Possible Causes: Overlay, animation, application state, disabled control
```

## Concurrency model

- JUnit runs tests in parallel. One test runs `beforeEach`, the body and `afterEach` on one thread.
- `ThreadLocal` holds the `TestContext`, web driver, mobile driver and cleanup stack.
- Shared structures are append-only or atomic: `Jsonl` (synchronized writes), metrics (`LongAdder`), event bus (copy-on-write listener list).
- Drivers are created on first use, so API-only tests never start a browser.

## Extension points

| To add | Implement |
|---|---|
| A new AI provider | `AiProvider`, registered in `META-INF/services/com.testora.ai.providers.AiProvider` |
| A new event consumer | `EventBus.global().subscribe(...)` |
| A new wait condition | `ElementCondition.of("name", element -> ...)` |
| Another automation tool (Playwright, visual, accessibility) | New package that publishes the same events and uses `TestContext` |

## Key decisions

| Decision | Reason |
|---|---|
| Single Maven module | Split only when a second consumer exists. Avoids premature complexity |
| Events instead of direct calls | Logging, reports and metrics can change without touching engines |
| Rules before AI | Cheaper, faster, reproducible, explainable |
| "Controlled recovery", not "self-healing" | Recovery must be visible, bounded and governed. It must not hide defects |
| Test retry left to Surefire | Retried tests stay visible in reports |
