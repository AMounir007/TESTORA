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

### AI and governance



| Key | Default | Description |
|---|---|---|
| `ai.mode` | `offline` | `normal`, `ai-unavailable` or `offline`. Only `normal` ever contacts a provider |
| `ai.timeout.seconds` | `20` | Per-call timeout |
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
