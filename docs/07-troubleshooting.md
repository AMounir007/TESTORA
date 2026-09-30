# 07 - Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `No TestContext bound to this thread` | Test class lacks `@Testora`, or code runs on another thread | Add `@Testora` (`com.testora.execution.Testora`). Do not start your own threads in tests |
| `Missing secret: X` | Environment variable not set | Set it in the same shell that runs Maven (`set X=...` or `export X=...`), or pass `-DX=...` |
| `Synchronization Timeout` | Element not ready within 15 s | Read the diagnostics in the message: Present / Visible / Enabled lines show where it stopped. Check the locator and any overlay, then look at the failure screenshot |
| `Unsupported browser` | Bad `browser` value | Use `chrome`, `firefox` or `edge` |
| `session not created` | Browser/driver mismatch or Grid unreachable | Update the browser. For Grid, check `grid.url` opens in a browser |
| `Invalid grid.url` / `Invalid appium.url` | Malformed URL | Include `http://` and port |
| Mobile session fails | Appium not running, device offline, wrong capabilities | Start Appium. Check `mobile.platform`, `mobile.device`, `mobile.app`. Run `adb devices` for Android |
| No report files | Tests did not run, or run aborted | Look in `target/testora/`. The HTML report is written at JVM exit |
| Everything is skipped | `-Dgroups=` tag matches nothing | Check your `@Tag` names |
| `ExampleTests` do nothing | Class is `@Disabled` | Configure your application, adapt locators, remove `@Disabled` |
| AI never runs | `ai.mode` is `offline` (default), no provider registered, or rule confidence is high enough | Set `-Dai.mode=normal` and register an `AiProvider` |
| Recovery never happens | `recovery.enabled` is false, or no `.withFallback(...)` | Enable it and declare fallbacks |
| Flaky failures | Shared state or real timing issue | Look for retries in the report. Check the fingerprint group. Use `-Dtestdata.seed` to rule out data differences |
| Maven cannot resolve dependencies | Network or proxy | Configure Maven proxy in `~/.m2/settings.xml` |
| Trends section is empty | No history yet, fewer than 5 runs, or history is lost on CI | Run the suite several times. Cache `.testora/history.jsonl` in CI. Check `history.enabled` |
| No "API contract coverage" section | `openapi.spec` not set, or the file cannot be read | Set `-Dopenapi.spec=...`. The section shows the reason if loading fails |
| Endpoint shows as "not called" though you call it | Path was filled in (`/customers/42`) instead of the spec template | Call `delete("/customers/{id}", id)` with path parameters |
| `Not an OpenAPI 3 document` | Swagger 2 file or wrong file | Convert to OpenAPI 3 |
| Generated contract scenario fails on a valid request | Missing data (404/409) or spec out of date | Review the scenario and the spec before calling it a defect |
| Selenium / Appium version errors | Library version mismatch | Align `selenium.version` and `appium.version` in `pom.xml` (see the Appium client's required Selenium version) |
| `Invalid wait.web.timeout = 'abc'` (or another `wait.*` key) | Bad duration, boolean or number in a wait setting | Use `300ms`, `15s` or `2m`. Polling must not exceed the timeout. See [03](03-configuration.md) |
| Timeouts too short on a slow environment or device | Default waits (web 15 s, mobile 30 s) are too low for that environment | Raise `wait.<channel>.timeout` for that environment only, then read the wait durations in the report |
| API call hangs or fails with a timeout | `wait.api.timeout` (default 30 s) reached | Check the service. Raise `wait.api.timeout` only if the endpoint is legitimately slow |
| Example tests fail with connection or timeout errors | No internet, proxy, or the public practice site is down | Open the site in a browser. Set a proxy if needed. Re-run later. They are not part of the smoke suite |
| Example test fails on a locator or text | The third-party site changed | Inspect the page and update the locator or expected text in `WebExampleTests` |
| `ApiExampleTests` read-after-write checks fail | JSONPlaceholder does not store writes | Expected. Use your own API to verify created data |
| Red errors in the IDE but Maven succeeds | Stale IDE index | Maven tool window: Reload All Maven Projects. Then File, Invalidate Caches, Invalidate and Restart |
| A source file looks scrambled (lines in reverse order) or a class is "not found" | A file was damaged by a failed edit | Restore it from Git (`git checkout -- <file>`) or ask for it to be rewritten, then run `mvn test-compile` |

## Good bug-report checklist

1. The exact command you ran.
2. The text of the exception (it already contains diagnostics).
3. Files from `target/testora/evidence/<executionId>/` (masked).
4. The matching row from `report.jsonl`.
5. Java, Maven and browser versions.
