# 08 - Status and Roadmap

This page is deliberately honest about what exists.

## Implemented (starter)

| Area | Status |
|---|---|
| Core: context, event bus, exceptions, masking | Implemented |
| Configuration (YAML, properties, environment, secrets) | Implemented |
| Synchronization: FluentWait engine, conditions, diagnostics, stale recovery | Implemented |
| Web: drivers (local/Grid), SmartElement, pages, components, browser actions | Implemented |
| API: RestAssured client, auth, upload, schema check, chaining | Implemented |
| Mobile: Android/iOS sessions, gestures, app lifecycle | Implemented (needs a real Appium setup to verify) |
| JUnit integration, cleanup, evidence, HTML and JSONL reports | Implemented |
| Telemetry, "why" explanations, preflight | Implemented |
| Failure classification and fingerprints | Implemented (rule-based, limited categories) |
| Flaky analysis, risk scoring, test selection, knowledge graph | Implemented as libraries. You supply the inputs |
| AI gateway, failure analysis | Implemented. No provider is bundled |
| Governed locator recovery | Implemented (fallback locators only) |
| CI templates: GitHub Actions, GitLab CI, Jenkins | Implemented |

## Known limitations

- The project has not yet been built or run in the authoring environment. Run `mvn test -Dgroups=smoke` first and report any compile or dependency issues.
- Wait timeouts and polling are code defaults, not YAML-configurable yet.
- Only a web wait policy exists. API and mobile policies are planned.
- The `suite` property in `pom.xml` is unused. Select tests with `-Dgroups`.
- Test history is not persisted, so flaky analysis needs you to pass the history in.
- `Preflight` and the intelligence classes are not wired into the test run automatically.
- Dependency versions are pinned but have not been checked against the latest releases or security advisories.
- Approval items are recorded only. There is no review interface.

## Not implemented yet

OpenAPI/contract analysis, AI test and test-data generation, requirement-to-test mapping, persisted execution history, Shadow DOM helpers, per-channel wait policies in YAML, self-generated documentation, Docker Compose for Grid and Appium, Playwright/visual/accessibility plugins, distributed workers, AI-proposed locator candidates.

## Suggested next steps

1. Build and run the smoke tests. Fix any dependency alignment issues.
2. Point `qa.yaml` at a real application. Enable and adapt `ExampleTests`.
3. Add a persisted run history, then feed it to the flakiness and selection classes.
4. Make wait policies configurable for web, API and mobile.
5. Add an OpenAPI reader.
6. Add one AI provider behind the gateway, starting with non-sensitive data.
