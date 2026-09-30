# 03 - Configuration

## How configuration is loaded

1. The environment name comes from `-Denv=<name>` (default `qa`).
2. File `src/main/resources/config/<env>.yaml` is read (flat `key: value` entries).
3. Each key is resolved in this order (first match wins):
   1. Java system property: `-Dkey=value`
   2. Environment variable: key upper-cased, dots replaced by underscores (`base.url` becomes `BASE_URL`)
   3. YAML file
   4. Built-in default

To add an environment, copy `qa.yaml` to `uat.yaml` and run with `-Denv=uat`.

## Settings reference

### Application and browser

| Key | Default | Description |
|---|---|---|
| `base.url` | empty | Web application root. `BasePage.open("/x")` appends to it |
| `api.base.url` | empty | Root for your `ApiClient` subclasses (when you read it in your client) |
| `browser` | `chrome` | `chrome`, `firefox` or `edge` |
| `headless` | `true` | Run without a visible window |
| `grid.url` | empty | Selenium Grid / Docker / cloud URL. Empty means run locally |

### Mobile

| Key | Default | Description |
|---|---|---|
| `mobile.platform` | `android` | `android` or `ios` |
| `mobile.device` | empty | Device name |
| `mobile.app` | empty | Path or URL of the app under test |
| `mobile.autoGrantPermissions` | `true` | Android only |
| `appium.url` | `http://127.0.0.1:4723` | Appium server or cloud endpoint |

### Reliability

| Key | Default | Description |
|---|---|---|
| `retry.action.max` | `2` | Attempts for one interaction (minimum 1) |
| `recovery.enabled` | `false` | Turns on governed locator recovery. See [05](05-ai-and-governance.md) |
| `testdata.seed` | random | Fixed seed for reproducible data |

### History and contracts

| Key | Default | Description |
|---|---|---|
| `history.enabled` | `true` | Record every test result |
| `history.file` | `.testora/history.jsonl` | History location (outside `target/`) |

See [09](09-execution-history.md) and [10](10-api-contracts.md).

| `openapi.spec` | empty | OpenAPI file (classpath or path). Enables the coverage section in the report |
| `history.min.runs` | `5` | Minimum runs before flakiness is reported |
| `history.flaky.threshold` | `0.10` | Minimum failure rate to call a mixed-result test flaky |
| `history.window` | `20` | Recent runs per test used for trends |
### AI and governance



| Key | Default | Description |
|---|---|---|
| `ai.mode` | `offline` | `normal`, `ai-unavailable` or `offline`. Only `normal` ever contacts a provider |
Polling and API settings are read when first used in a run, so set them before the tests start.

Invalid values stop the run with a clear message (for example `Invalid wait.web.timeout = 'abc'`, or polling longer than the timeout). Longer timeouts can hide real slowness, so raise them only where an environment is genuinely slower, and read the wait durations in the report.

```
wait.api.timeout: 10s
wait.mobile.timeout: 45s
wait.web.polling: 250ms
wait.web.timeout: 20s
```yaml

Durations are written `300ms`, `15s` or `2m`. A plain number means seconds. Example:

| `wait.web.maxStaleRecoveries` | `3` | `3` | `0` | Reserved upper bound for stale recoveries (not yet enforced separately) |
| `wait.web.ignoreNoSuchElement` | `true` | `true` | `false` | Keep polling while the element is absent |
| `wait.web.ignoreStale` | `true` | `true` | `false` | Re-locate stale elements while waiting |
| `wait.web.scriptTimeout` | `30s` | `60s` | `30s` | Script execution limit (web and mobile) |
| `wait.web.pageLoadTimeout` | `30s` | `60s` | `30s` | Browser page load limit (web only) |
| `wait.web.polling` | `300ms` | `500ms` | `500ms` | Time between condition checks (must not exceed the timeout) |
| `wait.web.timeout` | `15s` | `30s` | `30s` | Maximum wait for a condition. For `api`: connection and response timeout of each call |
|---|---|---|---|---|
| Key (replace `web` with `mobile` or `api`) | Web default | Mobile default | API default | Meaning |

## Wait policy (per channel)
| `ai.max.prompt.chars` | `8000` | Prompts are truncated to this size after masking |
| `ai.max.calls` | `50` | Cap per run (cost control) |
| `ai.analysis.threshold` | `0.80` | AI is asked only when rule confidence is below this |
| `governance.auto.threshold` | `0.85` | At or above: automatic action allowed |
| `governance.review.threshold` | `0.60` | At or above (and below auto): human review. Below: recommendation only |

## Wait policy

Default web policy (in `WaitPolicy.webDefaults()`): timeout 15 s, polling 300 ms, page load 30 s, script 30 s, stale and no-such-element exceptions ignored. These values live in one place only. They are **not yet YAML-configurable**. See [08 - Status and Roadmap](08-status-and-roadmap.md).

## Secrets

Never put passwords, tokens or keys in YAML or code.

```java
String password = TestoraConfig.get().secret("DEMO_PASSWORD");
```

`secret(...)` reads a system property or environment variable and fails with a clear message if it is missing. In CI, store it as a pipeline secret (GitHub Actions secrets, GitLab CI variables, Jenkins credentials).

## Parallel execution

Set in `src/test/resources/junit-platform.properties`:

```
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.config.fixed.parallelism=4
```

Change `parallelism` to the number of tests you want at once. Each test has its own context and driver.

## Logging

`src/test/resources/logback-test.xml`. Set `testora.events` to `DEBUG` to see every lifecycle event.
