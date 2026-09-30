# 08 - Status and Roadmap

This page is deliberately honest about what exists.

## Implemented (starter)

| Area | Status |
|---|---|
| Core: context, event bus, exceptions, masking | Implemented |
| Configuration (YAML, properties, environment, secrets) | Implemented |
| Synchronization: FluentWait engine, conditions, diagnostics, stale recovery | Implemented |
| Configurable wait policies for web, mobile and API (YAML, `-D`, environment) | Implemented. See [03](03-configuration.md) |
| Web: drivers (local/Grid), SmartElement, pages, components, browser actions | Implemented |
| API: RestAssured client, auth, upload, schema check, chaining | Implemented |
| Mobile: Android/iOS sessions, gestures, app lifecycle | Implemented (needs a real Appium setup to verify) |
| JUnit integration, cleanup, evidence, HTML and JSONL reports | Implemented |
| Telemetry, "why" explanations, preflight | Implemented |
| Failure classification and fingerprints | Implemented (rule-based, limited categories) |
| Execution history and trends (flaky, broken, new vs recurring failures, hidden retries) | Implemented (JSONL file). See [09](09-execution-history.md) |
| OpenAPI contract analysis (scenario generation, runner, endpoint coverage) | Implemented for a subset of OpenAPI 3. See [10](10-api-contracts.md) |
| Risk scoring, test selection, knowledge graph | Implemented as libraries. You supply the inputs |
| AI gateway, failure analysis | Implemented. No provider is bundled |
| Governed locator recovery | Implemented (fallback locators only) |
| CI: GitHub Actions (build, smoke, history cache, dependency report), Dependabot, GitLab CI, Jenkins | Implemented. Not yet run on GitHub. See [11](11-continuous-integration.md) |
| Runnable example tests for Web and API against public practice sites | Implemented (`WebExampleTests`, `ApiExampleTests`). Not yet run. See [12](12-example-tests.md) |

## Known limitations

- **Not verified by a build.** The code was written without a working build in the authoring environment, and several files had to be rewritten after failed edits damaged them. Run `mvn -B test-compile`, then `mvn -B test -Dgroups=smoke` (expected: 18 tests pass) and report any errors.
- `maxStaleRecoveries` is parsed and validated but not enforced as a separate limit (stale elements are re-located on every poll within the timeout).
- Wait policies are configurable per channel, but per-test or per-element overrides are not supported.
- The `suite` property in `pom.xml` is unused. Select tests with `-Dgroups`.
- History is stored in a local file (`.testora/history.jsonl`). On CI agents it is lost unless you cache it between runs. Trends need roughly 10 or more runs per test to be meaningful.
- Contract analysis supports a subset of OpenAPI 3 only (no Swagger 2, nested objects, `oneOf`/`anyOf`/`allOf`, remote `$ref`). Business rules cannot be derived from a spec. Endpoint coverage only matches calls made with the spec path template.
- `Preflight`, the risk scorer, the selector and the knowledge graph are not wired into the test run automatically.
- The failure classifier treats any message containing "401" or "403" as an authentication issue, which can misclassify unrelated numbers.
- Two classes are named `Testora` (`core.lifecycle.Testora` and the `@Testora` annotation in `execution`). They compile but can confuse readers.
- Dependency versions are pinned but have not been checked against the latest releases or security advisories.
- Approval items are recorded only. There is no review interface.
- There are no runnable Mobile examples (launch, login, gestures, permissions) because they need an Appium server and a device. There is no API authentication example against a public service.
- The Web and API examples depend on third-party public sites and need internet access. They are excluded from the offline smoke suite and from CI.

## Not implemented yet

AI test and test-data generation, AI suggestions for missing API scenarios, requirement-to-test mapping, Swagger 2 and advanced OpenAPI support, Shadow DOM helpers, self-generated documentation, Docker Compose for Grid and Appium, Playwright/visual/accessibility plugins, distributed workers, AI-proposed locator candidates, a command-line switch for history-based test selection, runnable Mobile examples.

## Suggested next steps

1. Run `mvn -B test-compile` and `mvn -B test -Dgroups=smoke`. Fix any errors.
2. Push to GitHub and read the first CI run.
3. Run the public examples (`mvn test -Dgroups=example`) and fix any locator drift.
4. Point `qa.yaml` at a real application. Enable and adapt `ExampleTests`.
5. Tune `wait.*` settings per environment after reading real wait durations in the report.
6. Add runnable Mobile examples and a Docker Compose file for Selenium Grid and Appium.
7. Add one AI provider behind the gateway, starting with non-sensitive data.
