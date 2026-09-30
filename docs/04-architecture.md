# 04 - Architecture
| Test retry left to Surefire | Retried tests stay visible in reports |
| "Controlled recovery", not "self-healing" | Recovery must be visible, bounded and governed. It must not hide defects |
| Rules before AI | Cheaper, faster, reproducible, explainable |
| Events instead of direct calls | Logging, reports and metrics can change without touching engines |
| Single Maven module | Split only when a second consumer exists. Avoids premature complexity |
|---|---|
| Decision | Reason |

## Key decisions

| Another automation tool (Playwright, visual, accessibility) | New package that publishes the same events and uses `TestContext` |
| A new wait condition | `ElementCondition.of("name", element -> ...)` |
| A new event consumer | `EventBus.global().subscribe(...)` |
| A new AI provider | `AiProvider`, registered in `META-INF/services/com.testora.ai.providers.AiProvider` |
|---|---|
| To add | Implement |

## Extension points

- Drivers are created on first use, so API-only tests never start a browser.
- Shared structures are append-only or atomic: `Jsonl` (synchronized writes), metrics (`LongAdder`), event bus (copy-on-write listener list).
- `ThreadLocal` holds the `TestContext`, web driver, mobile driver and cleanup stack.
- JUnit runs tests in parallel. One test runs `beforeEach`, the body and `afterEach` on one thread.

## Concurrency model

```
  Possible Causes: Overlay, animation, application state, disabled control
  Last State: Element present but not Enabled
  Attempts: 42
    Enabled = NO
    Visible = YES
    Present = YES
  Conditions:
  Polling: 300 ms
  Timeout: 15 seconds
  Locator: By.id: loginButton
  Element: Login Button
  Operation: CLICK
Synchronization Timeout
```

- On timeout throws `SynchronizationException` with a diagnostics report:
- Re-locates the element on every poll, so stale elements are handled.
- Returns as soon as the conditions are true (never waits the full timeout unnecessarily).
- Uses Selenium `FluentWait` with timeout and polling from the channel's `WaitPolicy` (web, mobile or api; see [03](03-configuration.md)).

```
locate -> present? -> visible? -> enabled? -> interact
```

`SmartElement.click()` does one polling loop:

## Synchronization

Holds execution ID, correlation ID, test ID, environment, start/end time, shared data, actions, evidence paths, retry and recovery counts and the AI analysis. It is bound to the current thread, so parallel tests cannot see each other's data.

## TestContext

```
at JVM exit: write report.html (with trends and API coverage), log telemetry summary
             TEST_PASSED, TEST_COMPLETED, write report row, append to history
afterEach  : CLEANUP_STARTED, run cleanups (always), quit drivers,
on failure : capture evidence, classify, optional AI analysis, TEST_FAILED
test body  : actions publish ACTION_*, WAIT_*, RETRY_*, RECOVERY_* events
beforeEach : create TestContext, TEST_STARTED, SETUP_STARTED
```

## Test lifecycle

| `testdata` | `DataGen` |
| `governance` | `ConfidencePolicy`, `ApprovalQueue`, `AuditTrail` |
| `ai` | `gateway`, `providers` (SPI), `diagnosis`, `recovery` |
| `intelligence` | `fingerprinting`, `impact` (flakiness), `risk`, `selection`, `knowledge` |
| `observability` | `Metrics`, `MetricsListener`, `Explainer` |
| `reporting` | `ReportWriter`, `TestSummary` |
| `evidence` | `EvidenceService` |
| `execution` | `@Testora`, `TestoraExtension`, `CleanupRegistry`, `Preflight` |
| `mobile` | `driver`, `gestures`, `devices` (app lifecycle) |
| `api.clients` | `ApiClient` |
| `web` | `driver` (factory, manager), `elements` (SmartElement), `pages`, `components`, `browser` |
| `synchronization` | `WaitEngine`, `Conditions`, `WaitDiagnostics`, `RetryPolicy` |
| `config` | `TestoraConfig`, `WaitPolicy` |
| `core.utilities` | `Masker` (secret masking), `Jsonl` (thread-safe writer) |
| `core.lifecycle` | `Testora.bootstrap()` one-time startup |
| `core.exceptions` | `TestoraException`, `SynchronizationException` |
| `core.events` | `EventBus`, `EventType`, `TestoraEvent`, `Events` helper |
| `core.context` | `TestContext` (per-test state), `TestContextHolder` (thread binding) |
|---|---|
| Package (`com.testora....`) | Responsibility |

## Packages

AI modes: **normal** (all levels), **ai-unavailable** and **offline** (levels 1 and 2). Tests behave the same in every mode.

| 3 - AI | Failure analysis (implemented), generation and more (planned) | Optional |
| 2 - Intelligent (rules and history) | Failure classifier, fingerprinting, flakiness, risk, selection, knowledge graph | No |
| 1 - Deterministic | Selenium, RestAssured, Appium, waits, config, data | No |
|---|---|---|
| Level | Examples | Needs AI? |

## Three intelligence levels

The core (`core`, `config`, `synchronization`) contains no business or domain terms. Business logic lives above it.

```
Every layer publishes events --> EventBus --> logging, metrics, explainer, reporting, evidence, AI analysis

Selenium   RestAssured   Appium
   |
WaitEngine (FluentWait)  <-- all synchronization lives here
   |
SmartElement   ApiClient   Gestures   BrowserActions
   |
Page / Component / API client / Screen objects
   |
Business components (optional, yours)       e.g. customer.create(...)
   |
Your tests
```

## Layers

