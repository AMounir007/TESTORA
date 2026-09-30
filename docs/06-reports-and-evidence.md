# 06 - Reports and Evidence

All output is under `target/testora/`.

| File | Audience | Content |
|---|---|---|
| `report.html` | Humans | Totals, failure groups by fingerprint, per-test table with status, duration, retries, recoveries, category, evidence count, "why" notes |
| `report.jsonl` | Tools | One JSON line per test (same data as the table) |
| `audit.jsonl` | Reviewers | AI calls, recovery decisions, approval requests |
| `evidence/<executionId>/` | Investigators | Screenshots, page source, URL, API request and response text |

Everything written is passed through the masker: passwords, tokens, API keys, `Authorization` headers and bearer tokens are replaced with `****`.

## Reading the HTML report

1. **Failure fingerprints**: each entry is one root cause and how many tests share it. Investigate groups, not individual tests.
2. **Trends**: flaky, broken and "passed only with retries" tests, and whether a failure cause is new or recurring. See [09](09-execution-history.md).
3. **API contract coverage**: which spec endpoints your tests never called. See [10](10-api-contracts.md).
2. **Category and confidence**: rule-based guess. Low confidence means "look at the evidence yourself".
3. **Retries and Recoveries**: non-zero values mean the test needed help. Review them. A passing test with retries may still hide a problem.
4. **Why** column: human-readable explanations of waits, retries and recoveries.

## Evidence captured on failure

| Channel | Captured |
|---|---|
| Web | Screenshot, current URL, page source (DOM) |
| Mobile | Screenshot, page source |
| API | For every call: method, URL, masked request headers, status, response time, response body |

Evidence capture is best effort and never fails a test.

## "Why" mode

Questions TESTORA can answer from recorded events:

| Question | Example answer |
|---|---|
| Why did it wait? | `Waited on CLICK 'Login Button': reason=Element present but not Enabled, polling=300ms, attempts=4, duration=1200ms, result=SUCCESS` |
| Why did it retry? | `Retried CLICK 'Login Button' because ElementClickInterceptedException (attempt 1)` |
| Why did it recover? | `Recovery started for 'Login Button' (original locator By.id: loginButton failed)` then `Recovery result: SUCCESS` |

They appear in the report's Why column and through `Explainer.why(executionId)`.

## Telemetry

A summary line is logged at the end of each run (logger `testora`):

`tests, failed, average test time, average wait time, average wait attempts, share of test time spent waiting, retries per test, recoveries, average driver creation time, average API time, AI calls and average AI time`.

These are measurements of your run, not benchmarks. To compare changes, run the same suite several times and compare medians.

## Preflight (environment readiness)

`Preflight.run("https://app.example.com", "https://api.example.com/health")` returns `Execution Ready` or `Execution Blocked` with reasons (Java version, disk space, unreachable or 5xx URLs). Call it before a large suite so a broken environment does not produce hundreds of misleading failures.
