# TESTORA — Quality Engineering Platform

[![testora](https://github.com/AMounir007/TESTORA/actions/workflows/ci.yml/badge.svg)](https://github.com/AMounir007/TESTORA/actions/workflows/ci.yml)


[![Release](https://github.com/AMounir007/TESTORA/actions/workflows/release.yml/badge.svg)](https://github.com/AMounir007/TESTORA/actions/workflows/release.yml)
[![JitPack](https://jitpack.io/v/AMounir007/TESTORA.svg)](https://jitpack.io/#AMounir007/TESTORA)

Deterministic first. Intelligent second. AI third.

## Use as a dependency (JitPack)

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.AMounir007</groupId>
  <artifactId>TESTORA</artifactId>
  <version>1.0.0</version> <!-- use the latest release tag -->
</dependency>
```

## Releasing

Releases are automatic. Every push to `main` runs `.github/workflows/release.yml`, which bumps the patch version
(1.0.0 → 1.0.1 → …), commits `pom.xml` with `[skip ci]`, creates the tag (no `v` prefix), publishes a GitHub Release
(notes from the matching `## [x.y.z]` section of `CHANGELOG.md`, otherwise auto-generated) and triggers JitPack.
To start from a higher number, change `BASE_VERSION` in the workflow. If a release fails, fix the cause, delete the tag
(`git push origin :refs/tags/<version>`) and the GitHub Release, then push again.


**Full documentation: [docs/README.md](docs/README.md)** — Getting Started, Writing Tests, Configuration, Architecture,
AI and Governance, Reports and Evidence, Troubleshooting, Status and Roadmap, Execution History, API Contracts, Continuous Integration, Example Tests ([docs/12](docs/12-example-tests.md): runnable Web and API examples to copy).

Quick start: `mvn test -Dgroups=smoke` (offline platform tests, no browser or AI needed).

Highlights: shared Web/API/Mobile context, central wait engine, masked evidence, failure fingerprints,
execution history with flaky-test trends ([docs/09](docs/09-execution-history.md)),
OpenAPI scenario generation and endpoint coverage ([docs/10](docs/10-api-contracts.md)),
optional AI behind a gateway (off by default).

## 1. Executive overview
TESTORA is a Java 21 platform for Web (Selenium), API (RestAssured) and Mobile (Appium) automation with one shared
`TestContext`, one synchronization engine, one event bus, and optional AI behind a gateway.
Testers write `loginPage.login(...)`; drivers, waits, retries, evidence, reporting and cleanup are automatic.

## 2. Challenges to the original vision (and what changed)
| Requirement | Decision |
|---|---|
| "Self-healing" | Renamed **Controlled Recovery**: disabled by default, evidence + confidence + policy + audit; never edits tests/assertions |
| "AI everywhere" | AI only for low-confidence failure analysis; rules handle the rest. Offline mode is the default |
| Single risk score | Risk only *orders* tests; weights are explicit, breakdown is returned (`RiskScorer`) |
| One module per concept (30+ packages) | One Maven module, packages by responsibility; split into modules only when a second consumer exists (over-engineering risk) |
| Adaptive polling | Fixed, configurable polling for now (FluentWait). Adaptive polling is an extension point, not shipped without benchmark data |
| Test retry | Not implemented in-framework: use surefire `rerunFailingTestsCount`; retries must stay visible, not hidden |
| Dependencies: "latest" | Versions pinned in `pom.xml`; verify against Maven Central/CVE feeds before adoption |

## 3-5. Architecture and responsibilities
```
Tests -> Business components (optional, yours) -> Page/API/Screen objects
      -> SmartElement / ApiClient / Gestures -> WaitEngine (FluentWait)
      -> Selenium / RestAssured / Appium
Everything publishes events -> EventBus -> logging, metrics, explainer, report, evidence, AI analysis
```
| Package | Responsibility |
|---|---|
| `core` | TestContext (thread-isolated), EventBus, exceptions, masking |
| `config` | YAML per env + system props + env vars; wait policy; secrets from env only |
| `synchronization` | WaitEngine, conditions, diagnostics, RetryPolicy (action) |
| `web` / `mobile` / `api` | Engines; drivers are lazy, per-thread, auto-released |
| `evidence` / `reporting` / `observability` | Masked evidence, JSONL + HTML report, telemetry, Why mode |
| `intelligence` | Classifier + fingerprint, flakiness, risk, selection, knowledge graph |
| `ai` | Gateway (provider SPI, fallback, timeout, limits, audit), failure analysis, locator recovery |
| `governance` | Confidence policy, approval queue, audit trail |
| `execution` | JUnit extension (`@Testora`), cleanup registry, preflight |

## 6. Dependencies
Java 21 LTS (release target; runs on newer JDKs). Selenium 4.27, JUnit 5.11, RestAssured 5.5, Appium java-client 9.3,
Logback/SLF4J, Jackson (YAML config + JSONL), AssertJ. **No AI SDK**: providers are plug-ins via `ServiceLoader`.
Check: Appium java-client pins its own Selenium range; if Maven reports a mismatch, align `selenium.version`.

## 7. Synchronization
`SmartElement.click()` → `WaitEngine.waitForElement`: locate → present → visible → enabled (early exit, one poll loop).
Stale elements are re-located each poll; click-intercepted/stale during the action triggers a bounded **action retry**
(`retry.action.max`). Three retry types are independent: synchronization (WaitPolicy), action (RetryPolicy), test (surefire).
Timeouts produce `SynchronizationException` with diagnostics (operation, locator, conditions, attempts, last state, causes).
No `Thread.sleep` anywhere.

## 8. Context and events
`TestContext` is created per test by `TestoraExtension`, bound to the thread (`TestContextHolder`), shared by Web/API/Mobile.
Events: TEST_STARTED … TEST_COMPLETED (see `EventType`). Listener failures never break tests.

## 9. Concurrency model
JUnit parallel (`junit-platform.properties`, 4 threads). One test = one thread for before/test/after, so `ThreadLocal`
context and drivers are isolated; no shared mutable state except append-only writers (`Jsonl`) and atomic counters.

## 10-12. AI, knowledge, governance
Modes: `ai.mode=normal | ai-unavailable | offline` (default offline). `AiGateway` masks prompts, caps size/time/calls,
falls back across providers, audits each call. AI output is labelled `[AI-INFERENCE, not verified]`; prompts require
FACT/INFERENCE/RECOMMENDATION/UNKNOWN. `ConfidencePolicy` thresholds (`governance.auto.threshold`, `governance.review.threshold`)
are config, not constants. Changes to assertions/tests go through `ApprovalQueue` (human decision).

## 13. Security
Secrets only from env vars/system props (`TestoraConfig.secret`). `Masker` applies to logs, evidence, reports, audit, AI prompts.
Nothing is sent to AI unless a provider is installed AND `ai.mode=normal`.

## 14. Performance
Measured, not claimed: `Metrics.summary()` prints driver creation, wait time share, API time, AI time at JVM exit.
Benchmark method: run the same suite N times with `-Dtestora...` variations, compare median wait share and framework overhead
from `target/testora/report.jsonl`.

## 15. Risks
| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| AI hallucination | High | Med | Rules first, labelled inference, evidence-only prompts, human approval |
| Recovery hides defects | High | Med | Off by default, audited, original failure recorded |
| Retry masks defects | High | Med | Bounded, emitted as events, reported per test |
| Thread safety | High | Low | ThreadLocal context/drivers, no shared state |
| Secret exposure | High | Med | Masking everywhere, env-only secrets |
| Dependency conflicts (Appium/Selenium) | Med | Med | Pinned versions, CI build |
| Over-engineering | Med | High | Single module, extension points only where needed |
| Mobile infra complexity | Med | High | Factory only; devices/cloud supplied via `appium.url` |

## 16. Running
```
mvn test -Denv=qa -Dgroups=smoke          # offline platform tests run anywhere
mvn test -Denv=qa -Dgrid.url=http://localhost:4444
```
Outputs: `target/testora/report.html`, `report.jsonl`, `audit.jsonl`, `evidence/<executionId>/`.
Config keys (yaml/system prop/ENV): `base.url api.base.url browser headless grid.url appium.url mobile.platform mobile.device
mobile.app ai.mode recovery.enabled retry.action.max governance.*`.

## 17. Roadmap / not yet implemented
Implemented as working starter: core, sync, web, api, mobile, extension, evidence, reporting, telemetry, classifier, fingerprinting,
flaky/risk/selection/graph, AI gateway + analysis, governed locator recovery.
**Not implemented yet** (extension points only): OpenAPI contract analysis, AI test/data generation, history persistence for
flaky detection, distributed workers, Shadow DOM helpers, Playwright/visual/a11y plugins, self-documentation generator,
Docker compose for Grid/Appium, AI-driven locator candidates. This code was written without running a build in this environment:
run `mvn test` first and fix any compile findings.

## 18. The 10-year question
Keep the core small and deterministic; make every intelligent feature optional, governed and explainable; depend on
interfaces (events, AI SPI, conditions) not vendors; measure framework overhead; never hide failures behind retries or AI.
