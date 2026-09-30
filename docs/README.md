# TESTORA Documentation

TESTORA is a Java 21 quality-engineering platform for **Web (Selenium)**, **API (RestAssured)** and **Mobile (Appium)** tests.
Testers write business-level steps. Drivers, waits, retries, evidence, reporting and cleanup are handled by the framework.

> Deterministic first. Intelligent second. AI third. Tests run fully without any AI service.

## Where to start

| I want to... | Read |
|---|---|
| Install and run my first test | [01 - Getting Started](01-getting-started.md) |
| Write Web, API, Mobile and cross-channel tests | [02 - Writing Tests](02-writing-tests.md) |
| Change settings (browser, URLs, AI, recovery) | [03 - Configuration](03-configuration.md) |
| Understand how it works inside | [04 - Architecture](04-architecture.md) |
| Understand AI, confidence and approvals | [05 - AI and Governance](05-ai-and-governance.md) |
| Read reports, evidence and "why" explanations | [06 - Reports and Evidence](06-reports-and-evidence.md) |
| Fix a problem | [07 - Troubleshooting](07-troubleshooting.md) |
| Know what is and is not built yet | [08 - Status and Roadmap](08-status-and-roadmap.md) |
| See flaky tests, new vs recurring failures, hidden retries | [09 - Execution History](09-execution-history.md) |
| Generate API tests from an OpenAPI spec, see endpoint coverage | [10 - API Contracts](10-api-contracts.md) |
| Run tests in CI, keep history, track dependency updates | [11 - Continuous Integration](11-continuous-integration.md) |
| Copy working example tests (Web, API) | [12 - Example Tests](12-example-tests.md) |

## Key ideas in 30 seconds

1. Add `@Testora` to a test class. It creates a per-test `TestContext`, captures evidence on failure, and always cleans up.
2. Use `SmartElement` for UI elements. Every click/type automatically waits for the element to be ready.
3. Use `ApiClient` subclasses for APIs. Calls are recorded with secrets masked.
4. Never write `Thread.sleep`. Never create or quit a driver yourself.
5. Secrets come from environment variables only.
