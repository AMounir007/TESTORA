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

## Good bug-report checklist

1. The exact command you ran.
2. The text of the exception (it already contains diagnostics).
3. Files from `target/testora/evidence/<executionId>/` (masked).
4. The matching row from `report.jsonl`.
5. Java, Maven and browser versions.
